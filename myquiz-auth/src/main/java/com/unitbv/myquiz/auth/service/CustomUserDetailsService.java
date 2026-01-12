package com.unitbv.myquiz.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.unitbv.myquiz.auth.dto.UserDetailsDTO;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final String iamServiceUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public CustomUserDetailsService(@Value("${MYQUIZ_IAM_URL}") String iamServiceUrl, RestTemplate restTemplate) {
        this.iamServiceUrl = iamServiceUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String url = iamServiceUrl + "/users/find/" + identifier;

        ResponseEntity<UserDetailsDTO> response = restTemplate.getForEntity(url, UserDetailsDTO.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return new User(
                    response.getBody().getUsername(), response.getBody().getHashedPassword(), new ArrayList<>()
            );
        }
        else throw new UsernameNotFoundException("User not found");

    }
}

