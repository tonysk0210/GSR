package com.hn2.core.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.glassfish.external.statistics.Stats;

/** 回應資訊 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseInfo {
  /** 回應時間 */
  private LocalDateTime time;
  /** 回應狀態 */
  @Builder.Default
  private State state = State.builder().build();

  /**
   * 以state資訊建構ResponseInfo物件,時間自動寫入
   *
   * @param code 1:正常 0:失敗
   * @param msgSubject 訊息簡述
   * @param msgContent 訊息詳述
   */
  public ResponseInfo(int code, String msgSubject, String msgContent) {
    State state = new State();
    state.setCode(code);
    state.setMsgSubject(msgSubject);
    state.setMsgContent(msgContent);

    this.time = LocalDateTime.now();
    this.state = state;
  }

  public ResponseInfo(int code, String msgSubject) {
    this(code, msgSubject, null);
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class State {
    /** 1:正常 0:失敗 */
    private int code;
    /** 訊息簡述 */
    private String msgSubject;
    /** 訊息詳述 */
    private String msgContent;
  }
}
