/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.api;

import java.util.List;

public record MemberListResponse(String cluster, List<Member> members) {
}
