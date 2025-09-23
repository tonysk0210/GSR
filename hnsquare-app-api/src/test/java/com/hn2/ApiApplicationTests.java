package com.hn2;

import static org.junit.jupiter.api.Assertions.assertEquals;

//import com.hn2.util.CryptoHelper;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApiApplicationTests {

//  @Autowired CryptoHelper cryptoHelper;
  @Autowired private StringEncryptor stringEncryptor;

  @Test
  public void contextLoads() {
    String _1qaz2wsx = stringEncryptor.encrypt("gT3!f_k3");
    System.out.println(_1qaz2wsx);
  }

  @Test
  public void cryptoTest() {
    String plainText = "CryptoTest";
//    String encryptedBase64 = cryptoHelper.encrypt(plainText);

//    String decrypted = cryptoHelper.decrypt(encryptedBase64);
    System.out.println(plainText);
//    System.out.println(decrypted);
//    assertEquals(plainText, decrypted);
  }
}
