package com.hn2.core.handler;

import com.hn2.core.dto.ResponseMessage;
import com.hn2.util.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Rest API 的 Exception 相關設定
 *
 * @author hsien
 */
@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
  /** 日期時間格式 */
  private static final String DATE_PATTERN = "yyyy-MM-dd hh:mm:ss";

  /**
   * 授權 Exception
   *
   * @param e AuthenticationException
   * @param httpServletRequest HttpServletRequest
   * @param httpServletResponse HttpServletResponse
   * @return ResponseEntity ResponseMessage
   */
//  @ExceptionHandler(AuthenticationException.class)
//  public ResponseEntity<Object> handleAuthenticationException(
//      AuthenticationException e,
//      HttpServletRequest httpServletRequest,
//      HttpServletResponse httpServletResponse) {
//    ResponseMessage data = new ResponseMessage();
//    data.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
//    data.setStatus(HttpStatus.UNAUTHORIZED.value());
//    data.setMessage(e.getMessage());
//    data.setPath(httpServletRequest.getRequestURL().toString());
//    return new ResponseEntity<>(data, new HttpHeaders(), HttpStatus.UNAUTHORIZED);
//  }

  /**
   * 一般 Exception
   *
   * @param e Exception
   * @param httpServletRequest HttpServletRequest
   * @param httpServletResponse HttpServletResponse
   * @return ResponseEntity ResponseMessage
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handle(
      Exception e, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
    log.error("Responding with exception:", e);
    if (e instanceof NullPointerException) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    ResponseMessage data = new ResponseMessage();
    data.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
    data.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    data.setMessage("Internal Server Error");
    data.setPath(httpServletRequest.getRequestURL().toString());
    return new ResponseEntity<>(data, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 業務邏輯 Exception
   *
   * @param e BusinessException
   * @param httpServletRequest HttpServletRequest
   * @param httpServletResponse HttpServletResponse
   * @return ResponseEntity ResponseMessage
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Object> handleBusinessException(
      BusinessException e,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse) {
    // 由Exception取狀態碼 若為空則預設為422
    Integer statusCode =
        Optional.ofNullable(e.getStatusCode()).orElse(HttpStatus.UNPROCESSABLE_ENTITY.value());

    ResponseMessage data = new ResponseMessage();
    data.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
    data.setStatus(statusCode);
    data.setMessage(e.getMessage());
    data.setPath(httpServletRequest.getRequestURL().toString());
    return new ResponseEntity<>(data, new HttpHeaders(), statusCode);
  }

  /**
   * 存取權限 Exception
   *
   * @param e AccessDeniedException
   * @param httpServletRequest HttpServletRequest
   * @param httpServletResponse HttpServletResponse
   * @return ResponseEntity ResponseMessage
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleAccessDeniedException(
      AccessDeniedException e,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse) {
    ResponseMessage data = new ResponseMessage();
    data.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
    data.setStatus(HttpStatus.FORBIDDEN.value());
    data.setMessage(e.getMessage());
    data.setPath(httpServletRequest.getRequestURL().toString());
    return new ResponseEntity<>(data, new HttpHeaders(), HttpStatus.FORBIDDEN);
  }

  /**
   * Http 400 控制
   *
   * @param e MethodArgumentNotValidException
   * @param headers HttpHeaders
   * @param status HttpStatus
   * @param request WebRequest
   * @return ResponseMessage
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    ResponseMessage data = new ResponseMessage();
    data.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
    data.setStatus(HttpStatus.BAD_REQUEST.value());
    List<ObjectError> allErrors = e.getBindingResult().getAllErrors();

    StringBuilder errMsg = new StringBuilder("錯誤訊息:\n");
    allErrors.forEach(
        err -> {
          errMsg.append(err.getDefaultMessage()).append("\n");
        });
    data.setMessage(errMsg.toString());
    data.setPath(getUriByWebRequest(request));
    return new ResponseEntity<>(data, HttpStatus.BAD_REQUEST);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    ResponseMessage data = new ResponseMessage();
    data.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
    data.setStatus(HttpStatus.BAD_REQUEST.value());
    data.setMessage("請求格式錯誤，無法解析請求。");
    log.error("[無法解析請求]: {}", ex.getMessage());
    data.setPath(getUriByWebRequest(request));
    return new ResponseEntity<>(data, HttpStatus.BAD_REQUEST);
  }

  private String getUriByWebRequest(WebRequest request) {
    String description = request.getDescription(true);
    String uri = Arrays.stream(description.split(";")).findFirst().orElse("");
    return uri.replace("uri=", "");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected ResponseEntity<Object> handleConstraintViolationException(
      ConstraintViolationException e, WebRequest request) {
    ResponseMessage data = new ResponseMessage();
    data.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
    data.setStatus(HttpStatus.BAD_REQUEST.value());

    StringBuilder errMsg = new StringBuilder("錯誤訊息:\n");
    e.getConstraintViolations()
        .forEach(
            err -> {
              errMsg.append(err.getMessage()).append("\n");
            });
    data.setMessage(errMsg.toString());
    data.setPath(getUriByWebRequest(request));
    return new ResponseEntity<>(data, HttpStatus.BAD_REQUEST);
  }

}
