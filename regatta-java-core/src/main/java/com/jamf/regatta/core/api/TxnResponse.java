/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.api;

import com.jamf.regatta.proto.ResponseOp;

import java.util.List;

public record TxnResponse(Header header, boolean succeeded, List<ResponseOp> responses) implements Response {
}
