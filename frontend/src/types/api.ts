export type Role = 'CUSTOMER' | 'STAFF' | 'MANAGER' | 'ADMIN';

export interface PageResponse<T> {
  content: T[];
  page: number;          // zero-based
  size: number;
  totalElements: number;
  totalPages: number;
  from: number;          // dùng cho "Showing from–to of totalElements"
  to: number;
}

export interface FieldErrorItem {
  field: string;
  message: string;
}

export type ErrorCode =
  | 'VALIDATION_FAILED'
  | 'MALFORMED_REQUEST'
  | 'INVALID_TOKEN'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'EMAIL_NOT_VERIFIED'
  | 'ACCOUNT_DISABLED'
  | 'RESOURCE_NOT_FOUND'
  | 'METHOD_NOT_ALLOWED'
  | 'CONFLICT'
  | 'PAYLOAD_TOO_LARGE'
  | 'BUSINESS_RULE_VIOLATION'
  | 'TOO_MANY_REQUESTS'
  | 'INTERNAL_ERROR';

export interface ApiError {
  timestamp: string;
  status: number;
  code: ErrorCode;
  message: string;
  errors?: FieldErrorItem[];
  path: string;
  requestId?: string;
}

export interface ListQueryParams {
  page?: number;
  size?: number;
  sort?: string; // dạng "price:desc,name:asc"
}