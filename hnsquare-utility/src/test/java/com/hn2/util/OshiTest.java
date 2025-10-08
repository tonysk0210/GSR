package com.hn2.util;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
//import org.junit.jupiter.api.Test;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OshiTest {

  private static final int OSHI_WAIT_SECOND = 1000;
  private static SystemInfo systemInfo = new SystemInfo();
  private static HardwareAbstractionLayer hardware = systemInfo.getHardware();
  private static OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

  public static JSONObject getCpuInfo() {
    JSONObject cpuInfo = new JSONObject();
    CentralProcessor processor = hardware.getProcessor();
    // CPU 資訊
    long[] prevTicks = processor.getSystemCpuLoadTicks();
    Util.sleep(OSHI_WAIT_SECOND);
    long[] ticks = processor.getSystemCpuLoadTicks();
    long nice =
        ticks[CentralProcessor.TickType.NICE.getIndex()]
            - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
    long irq =
        ticks[CentralProcessor.TickType.IRQ.getIndex()]
            - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
    long softirq =
        ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
            - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
    long steal =
        ticks[CentralProcessor.TickType.STEAL.getIndex()]
            - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
    long cSys =
        ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
            - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
    long user =
        ticks[CentralProcessor.TickType.USER.getIndex()]
            - prevTicks[CentralProcessor.TickType.USER.getIndex()];
    long iowait =
        ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
            - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
    long idle =
        ticks[CentralProcessor.TickType.IDLE.getIndex()]
            - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
    long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
    // cpu核數
    cpuInfo.put("cpuNum", processor.getLogicalProcessorCount());
    // cpu系統使用率
    cpuInfo.put("cSys", new DecimalFormat("#.##%").format(cSys * 1.0 / totalCpu));
    // cpu用戶使用率
    cpuInfo.put("user", new DecimalFormat("#.##%").format(user * 1.0 / totalCpu));
    // cpu當前等待率
    cpuInfo.put("iowait", new DecimalFormat("#.##%").format(iowait * 1.0 / totalCpu));
    // cpu當前使用率
    cpuInfo.put("idle", new DecimalFormat("#.##%").format(1.0 - (idle * 1.0 / totalCpu)));

    return cpuInfo;
  }

  /** 系統jvm信息 */
  public static JSONObject getJvmInfo() {
    JSONObject cpuInfo = new JSONObject();
    Properties props = System.getProperties();
    Runtime runtime = Runtime.getRuntime();
    long jvmTotalMemoryByte = runtime.totalMemory();
    long freeMemoryByte = runtime.freeMemory();
    // jvm總記憶體
    cpuInfo.put("total", formatByte(runtime.totalMemory()));
    // 空閒空間
    cpuInfo.put("free", formatByte(runtime.freeMemory()));
    // jvm最大可申請
    cpuInfo.put("max", formatByte(runtime.maxMemory()));
    // vm已使用記憶體
    cpuInfo.put("user", formatByte(jvmTotalMemoryByte - freeMemoryByte));
    // jvm記憶體使用率
    cpuInfo.put(
        "usageRate",
        new DecimalFormat("#.##%")
            .format((jvmTotalMemoryByte - freeMemoryByte) * 1.0 / jvmTotalMemoryByte));
    // jdk版本
    cpuInfo.put("jdkVersion", props.getProperty("java.version"));
    // jdk路徑
    cpuInfo.put("jdkHome", props.getProperty("java.home"));
    return cpuInfo;
  }

  /** 系統記憶體信息 */
  public static JSONObject getMemInfo() {
    JSONObject cpuInfo = new JSONObject();
    GlobalMemory memory = systemInfo.getHardware().getMemory();
    // 總記憶體
    long totalByte = memory.getTotal();
    // 剩餘
    long acaliableByte = memory.getAvailable();
    // 總記憶體
    cpuInfo.put("total", formatByte(totalByte));
    // 使用
    cpuInfo.put("used", formatByte(totalByte - acaliableByte));
    // 剩餘記憶體
    cpuInfo.put("free", formatByte(acaliableByte));
    // 使用率
    cpuInfo.put(
        "usageRate",
        new DecimalFormat("#.##%").format((totalByte - acaliableByte) * 1.0 / totalByte));
    return cpuInfo;
  }

  /** 系統硬碟信息 */
  public static JSONArray getSysFileInfo() {
    JSONObject cpuInfo;
    JSONArray sysFiles = new JSONArray();
    FileSystem fileSystem = operatingSystem.getFileSystem();
    List<OSFileStore> fsArray = fileSystem.getFileStores();
    for (OSFileStore fs : fsArray) {
      cpuInfo = new JSONObject();
      // 硬碟路徑
      cpuInfo.put("dirName", fs.getMount());
      // 硬碟類型
      cpuInfo.put("sysTypeName", fs.getType());
      // 文件類型
      cpuInfo.put("typeName", fs.getName());
      // 總大小
      cpuInfo.put("total", formatByte(fs.getTotalSpace()));
      // 剩餘大小
      cpuInfo.put("free", formatByte(fs.getUsableSpace()));
      // 已經使用量
      cpuInfo.put("used", formatByte(fs.getTotalSpace() - fs.getUsableSpace()));
      if (fs.getTotalSpace() == 0) {
        // 資源的使用率
        cpuInfo.put("usage", 0);
      } else {
        cpuInfo.put(
            "usage",
            new DecimalFormat("#.##%")
                .format((fs.getTotalSpace() - fs.getUsableSpace()) * 1.0 / fs.getTotalSpace()));
      }
      sysFiles.add(cpuInfo);
    }
    return sysFiles;
  }

  /** 系統信息 */
  public static JSONObject getSysInfo() throws UnknownHostException {
    JSONObject cpuInfo = new JSONObject();
    Properties props = System.getProperties();
    // 操作系統名
    cpuInfo.put("osName", props.getProperty("os.name"));
    // 系統架構
    cpuInfo.put("osArch", props.getProperty("os.arch"));
    // 服務器名稱
    cpuInfo.put("computerName", InetAddress.getLocalHost().getHostName());
    // 服務器Ip
    cpuInfo.put("computerIp", InetAddress.getLocalHost().getHostAddress());
    // 服務器Mac地址
    cpuInfo.put("computerMac", getMacAddr());
    // 項目路徑
    cpuInfo.put("userDir", props.getProperty("user.dir"));
    return cpuInfo;
  }

  /** 獲取系統的Mac地址 */
  public static String getMacAddr() throws UnknownHostException {
    String hostAddress = InetAddress.getLocalHost().getHostAddress();
    SystemInfo systemInfo = new SystemInfo();
    List<Object> list = new ArrayList<>();
    HardwareAbstractionLayer hardware = systemInfo.getHardware();
    List<NetworkIF> networkIFs = hardware.getNetworkIFs();
    to:
    for (NetworkIF networkIF : networkIFs) {
      String[] iPv4addr = networkIF.getIPv4addr();
      for (String s : iPv4addr) {
        if (hostAddress.equals(s)) {
          list.add(networkIF);
          break to;
        }
      }
    }
    NetworkIF networkIF = (NetworkIF) list.get(0);
    String macAddr = networkIF.getMacaddr();
    return macAddr;
  }

  /** 單位轉換 */
  private static String formatByte(long byteNumber) {
    // 換算單位
    double FORMAT = 1024.0;
    double kbNumber = byteNumber / FORMAT;
    if (kbNumber < FORMAT) {
      return new DecimalFormat("#.##KB").format(kbNumber);
    }
    double mbNumber = kbNumber / FORMAT;
    if (mbNumber < FORMAT) {
      return new DecimalFormat("#.##MB").format(mbNumber);
    }
    double gbNumber = mbNumber / FORMAT;
    if (gbNumber < FORMAT) {
      return new DecimalFormat("#.##GB").format(gbNumber);
    }
    double tbNumber = gbNumber / FORMAT;
    return new DecimalFormat("#.##TB").format(tbNumber);
  }

  /** 所有系統信息 */
  /*@Test
  public void getInfo() throws UnknownHostException {
    JSONObject info = new JSONObject();
    info.put("cpuInfo", getCpuInfo());
    info.put("jvmInfo", getJvmInfo());
    info.put("memInfo", getMemInfo());
    info.put("sysInfo", getSysInfo());
    info.put("sysFileInfo", getSysFileInfo());
  }*/
}
