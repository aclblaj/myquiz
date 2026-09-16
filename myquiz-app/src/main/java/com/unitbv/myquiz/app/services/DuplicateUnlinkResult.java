package com.unitbv.myquiz.app.services;

/** Explicit outcome of removing duplicate associations. */
public record DuplicateUnlinkResult(Status status, long removedLinks) {

    public enum Status {
        REMOVED,
        NO_LINKS,
        QUESTION_NOT_FOUND
    }

    public static DuplicateUnlinkResult questionNotFound() {
        return new DuplicateUnlinkResult(Status.QUESTION_NOT_FOUND, 0);
    }

    public static DuplicateUnlinkResult fromRemovedLinks(long removedLinks) {
        return new DuplicateUnlinkResult(
                removedLinks > 0 ? Status.REMOVED : Status.NO_LINKS,
                removedLinks
        );
    }
}
