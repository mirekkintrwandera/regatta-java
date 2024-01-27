/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.api;

import java.util.List;

public record Member(String id, String name, List<String> peerUrls, List<String> clientURLs) {
}
