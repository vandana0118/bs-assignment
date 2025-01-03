import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.net.MalformedURLException;
//import org.testng.annotations.*;

public class BaseClass {
    public static WebDriver driver;
    @BeforeClass
    public void setupDriver() throws MalformedURLException {

        WebDriverManager.chromedriver().clearDriverCache().setup();
        ChromeOptions chromeOptions=new ChromeOptions();
        chromeOptions.addArguments("start-maximized");
        driver=new ChromeDriver(chromeOptions);

    }
    public static WebDriver getWebDriver(){
        //return driver.get();
        return driver;
    }
    @AfterClass
    public  void closeBrowser() {

        driver.quit();
    }
}
