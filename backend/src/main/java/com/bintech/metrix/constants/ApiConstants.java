package com.bintech.metrix.constants;

/**
 * API响应相关常量
 */
public final class ApiConstants {

    private ApiConstants() {}

    /** API基础路径 */
    public static final String API_BASE_PATH = "/api";
    /** API路径匹配规则 */
    public static final String PATH_PATTERNS = API_BASE_PATH + "/**";

    /** 冻结用户异常码 */
    public static final int FROZEN_USER_CODE = 1001;


    /** 响应JSON字段名 */
    public static final String KEY_STATUS = "status";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_QUERY = "query";
    public static final String KEY_DATA = "data";
    public static final String KEY_CODE = "code";
    public static final String KEY_COUNT = "count";
    public static final String KEY_TITLE = "title";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_VALUE = "value";
    public static final String KEY_URL = "url";

    /** 状态值 */
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    /** 默认响应消息 */
    public static final String DEFAULT_SUCCESS_MSG = "Success";
    public static final String VALIDATION_FAILED_MSG = "Validation failed";
    public static final String INTERNAL_ERROR_MSG = "Internal server error";

    /** HTTP请求头 */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String AUTH_BEARER_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE_JSON = "application/json";

    /** 默认分页 */
    public static final String DEFAULT_PAGE = "1";
    public static final String DEFAULT_PAGE_SIZE = "20";
}
