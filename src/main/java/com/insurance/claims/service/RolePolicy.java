package com.insurance.claims.service;

import com.insurance.claims.domain.ClaimStatus;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.exception.ForbiddenException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class RolePolicy {
    public static final String CLAIM_ADMIN = "Claim Admin";
    public static final String CLAIM_ANALYST = "Claim Analyst";
    public static final String SYSTEM_ADMINISTRATOR = "System Administrator";

    private static final EnumSet<ClaimStatus> CLAIM_ADMIN_VISIBLE_STATUSES =
            EnumSet.of(ClaimStatus.FILED, ClaimStatus.UNDER_REVIEW);
    private static final EnumSet<ClaimStatus> DETAIL_EDITABLE_STATUSES =
            EnumSet.of(ClaimStatus.FILED, ClaimStatus.UNDER_REVIEW);
    private static final EnumSet<ClaimStatus> CLAIM_ADMIN_TARGET_STATUSES =
            EnumSet.of(ClaimStatus.UNDER_REVIEW, ClaimStatus.CLOSED);
    private static final EnumSet<ClaimStatus> CLAIM_ANALYST_TARGET_STATUSES =
            EnumSet.of(ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, ClaimStatus.REJECTED, ClaimStatus.CLOSED);

    private RolePolicy() {
    }

    public static List<String> roles() {
        return Arrays.asList(CLAIM_ADMIN, CLAIM_ANALYST, SYSTEM_ADMINISTRATOR);
    }

    public static void validateRole(String role) {
        if (!roles().contains(role)) {
            throw new ForbiddenException("Unsupported role: " + role);
        }
    }

    public static boolean canView(SystemUser user, ClaimStatus status) {
        if (isSystemAdministrator(user) || isClaimAnalyst(user)) {
            return true;
        }
        return isClaimAdmin(user) && CLAIM_ADMIN_VISIBLE_STATUSES.contains(status);
    }

    public static boolean canCreate(SystemUser user) {
        return isSystemAdministrator(user) || isClaimAnalyst(user) || isClaimAdmin(user);
    }

    public static boolean canEditDetails(SystemUser user, ClaimStatus status) {
        if (!DETAIL_EDITABLE_STATUSES.contains(status)) {
            return false;
        }
        if (isSystemAdministrator(user) || isClaimAnalyst(user)) {
            return true;
        }
        return isClaimAdmin(user) && CLAIM_ADMIN_VISIBLE_STATUSES.contains(status);
    }

    public static boolean canAddNote(SystemUser user, ClaimStatus status) {
        return canView(user, status);
    }

    public static boolean canMoveTo(SystemUser user, ClaimStatus status) {
        if (isSystemAdministrator(user)) {
            return true;
        }
        if (isClaimAnalyst(user)) {
            return CLAIM_ANALYST_TARGET_STATUSES.contains(status);
        }
        return isClaimAdmin(user) && CLAIM_ADMIN_TARGET_STATUSES.contains(status);
    }

    public static boolean canManageUsers(SystemUser user) {
        return isSystemAdministrator(user);
    }

    private static boolean isClaimAdmin(SystemUser user) {
        return user != null && CLAIM_ADMIN.equals(user.getRole());
    }

    private static boolean isClaimAnalyst(SystemUser user) {
        return user != null && CLAIM_ANALYST.equals(user.getRole());
    }

    private static boolean isSystemAdministrator(SystemUser user) {
        return user != null && SYSTEM_ADMINISTRATOR.equals(user.getRole());
    }
}
