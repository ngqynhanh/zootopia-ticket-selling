package com.zoopass.common.exception;

/**
 * 422 - the request is well-formed but breaks a business rule,
 * e.g. "Tour xe điện VIP cần tối thiểu 6 vé".
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
