package com.shoptourr

/**
 * Domain failures mapped by [com.shoptourr.web.ApiExceptionHandler] onto the public
 * problem-details contract. Keep these out of `web/` so services never import HTTP types.
 */
class ResourceConflictException(message: String) : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class AuthenticationFailedException(message: String) : RuntimeException(message)

class DomainValidationException(message: String) : RuntimeException(message)
