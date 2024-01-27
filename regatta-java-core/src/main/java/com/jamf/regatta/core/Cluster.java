/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core;

import com.jamf.regatta.core.api.MemberListResponse;

public interface Cluster extends CloseableClient {
    MemberListResponse memberList();
}
