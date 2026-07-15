package com.unitbv.myquiz.thy.controller;

import com.unitbv.myquiz.api.settings.ControllerSettings;
import com.unitbv.myquiz.api.util.PaginationParams;
import com.unitbv.myquiz.api.util.PaginationSupport;
import com.unitbv.myquiz.thy.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf controller for the per-duplicate-link "mark resolved" action. This flags a
 * duplicate pair as resolved without unlinking it (materially different from
 * {@link ThyQuestionController}'s selective/bulk unlink flows), mirroring
 * {@link ThyErrorController#resolveError} exactly.
 */
@Controller
@RequestMapping("/duplicates")
public class ThyQuestionDuplicateController {

    private static final Logger log = LoggerFactory.getLogger(ThyQuestionDuplicateController.class);

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    @Value("${MYQUIZ_API_BASE_URL}")
    private String apiBaseUrl;

    @Autowired
    public ThyQuestionDuplicateController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    @PostMapping("/{id}/mark-resolved")
    public String markResolved(@PathVariable Long id,
                               @RequestParam(value = "primaryQuestionId", required = false) Long primaryQuestionId,
                               @RequestParam(value = ControllerSettings.ATTR_BACK_URL, required = false) String backUrl,
                               @RequestParam(value = ControllerSettings.ATTR_COURSE_ID, required = false) Long courseId,
                               @RequestParam(value = ControllerSettings.ATTR_AUTHOR_ID, required = false) Long authorId,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = ControllerSettings.ATTR_QUESTION_BANK_ID, required = false) Long questionBankId,
                               @RequestParam(value = ControllerSettings.ATTR_PAGE_NUMBER, required = false) Integer page,
                               @RequestParam(value = ControllerSettings.ATTR_PAGE_SIZE, required = false) Integer pageSize,
                               RedirectAttributes redirectAttributes) {
        String redirect = sessionService.validateSessionOrRedirect();
        if (redirect != null) return redirect;

        log.info("Marking duplicate link {} as resolved (primaryQuestionId: {})", id, primaryQuestionId);
        try {
            HttpEntity<Void> entity = sessionService.createAuthorizedRequest();
            restTemplate.exchange(apiBaseUrl + ControllerSettings.API_DUPLICATES + "/" + id + "/resolve", HttpMethod.PUT, entity, Void.class);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_SUCCESS_MESSAGE, ControllerSettings.MSG_DUPLICATE_RESOLVED_SUCCESS);
            log.info("Duplicate link {} resolved successfully", id);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.warn("Permission denied for resolving duplicate link {}", id);
            sessionService.invalidateCurrentSession();
            return ControllerSettings.VIEW_REDIRECT_AUTH_LOGIN;
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Duplicate link not found with id: {}", id);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_DUPLICATE_RESOLVED_FAILED);
        } catch (Exception ex) {
            log.error("Error resolving duplicate link with id: {}", id, ex);
            redirectAttributes.addFlashAttribute(ControllerSettings.ATTR_ERROR_MESSAGE, ControllerSettings.MSG_DUPLICATE_RESOLVED_FAILED);
        }

        return "redirect:" + buildRedirectUrl(primaryQuestionId, backUrl, courseId, authorId, type, questionBankId, page, pageSize);
    }

    private String buildRedirectUrl(Long primaryQuestionId, String backUrl, Long courseId, Long authorId, String type,
                                    Long questionBankId, Integer page, Integer pageSize) {
        if (primaryQuestionId != null) {
            PaginationParams pagination = PaginationSupport.normalize(page, pageSize);
            StringBuilder url = new StringBuilder(ControllerSettings.QUESTIONS_PATH_PREFIX)
                    .append(primaryQuestionId).append(ControllerSettings.API_QUESTION_DUPLICATES);
            String sep = "?";
            if (courseId != null) {
                url.append(sep).append(ControllerSettings.ATTR_COURSE_ID).append("=").append(courseId);
                sep = "&";
            }
            if (questionBankId != null) {
                url.append(sep).append(ControllerSettings.ATTR_QUESTION_BANK_ID).append("=").append(questionBankId);
                sep = "&";
            }
            if (authorId != null) {
                url.append(sep).append(ControllerSettings.ATTR_AUTHOR_ID).append("=").append(authorId);
                sep = "&";
            }
            if (type != null && !type.isBlank()) {
                url.append(sep).append("type=").append(type);
                sep = "&";
            }
            url.append(sep).append(ControllerSettings.ATTR_PAGE_NUMBER).append("=").append(pagination.page());
            url.append("&").append(ControllerSettings.ATTR_PAGE_SIZE).append("=").append(pagination.pageSize());
            return url.toString();
        }

        if (backUrl != null && !backUrl.isBlank()) {
            return backUrl;
        }

        return ControllerSettings.API_QUESTIONS;
    }
}
