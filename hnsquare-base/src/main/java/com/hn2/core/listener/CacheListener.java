package com.hn2.core.listener;

import lombok.extern.slf4j.Slf4j;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheListener implements CacheEventListener<Object, Object> {
  @Override
  public void onEvent(CacheEvent<?, ?> event) {
    log.info(
        "Cache event = {}, Key = {},  Old value = {}, New value = {}",
        event.getType(),
        event.getKey(),
        event.getOldValue(),
        event.getNewValue());
  }
}
