import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;
import org.testng.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;

public class NewsOutletTests extends BaseClass{

    @Test
    public static void verifyElpaisArticles() {
        getWebDriver().get("https://elpais.com");
        acceptPolicies();
        verifyLanguage();
        navigateToOpinionPage();
        List<WebElement> firstFiveArticles = getArticles(5);
        String[] translatedTitles = processArticlesAndGetTranslations(firstFiveArticles);;
        printDuplicateWordsInTitles(translatedTitles);
    }

    private static void verifyLanguage(){
        String htmlLangAttr = getWebDriver().findElement(By.tagName("html")).getAttribute("lang");
        System.out.println("Html lang attr: " + htmlLangAttr);
        Assert.assertEquals(htmlLangAttr, "es-ES");

        //Verify Language selected by default
        getWebDriver().findElement(By.id("btn_open_hamburger")).click();
        String languageSelectorPath = "//div[@id='hamburger_container']//ul/li[@class='ed_c']/a/span";
        String selectedLanguage = getWebDriver().findElement(By.xpath(languageSelectorPath)).getText();
        System.out.println("selectedLanguage: " + selectedLanguage);
        Assert.assertEquals(selectedLanguage.toLowerCase(), "España".toLowerCase());
    }

    private static void acceptPolicies(){
        String acceptAndContinueBtnPath = "//a[text()='Accept and continue']";
        if(getWebDriver().findElements(By.xpath(acceptAndContinueBtnPath)).size()>0){
            getWebDriver().findElement(By.xpath(acceptAndContinueBtnPath)).click();
        }

        //Click Accept button
        String agreeBtnPath = "//button[@id='didomi-notice-agree-button']";
        if(getWebDriver().findElements(By.xpath(agreeBtnPath)).size()>0) {
            By agreeButton = By.xpath(agreeBtnPath);
            WebDriverWait wait = new WebDriverWait(getWebDriver(), Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(agreeButton));
            getWebDriver().findElement(agreeButton).click();
        }
    }


    private static void navigateToOpinionPage(){
        //Click Opinion link
        String opinionLinkPath = "//div[@class='me_f ']/ul/li/a[contains(@href,'opinion')]";
        WebElement opinionLink = getWebDriver().findElement(By.xpath(opinionLinkPath));
        Assert.assertEquals(opinionLink.getText().toLowerCase(), "OPINIÓN".toLowerCase());
        System.out.println("Spanish text: " + opinionLink.getText());
        opinionLink.click();
        try{
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static List<WebElement> getArticles(int maxLength){
        //Get List of all articles on the page
        By articleSelector = By.xpath("//section//article");
        WebDriverWait wait = new WebDriverWait(getWebDriver(), Duration.ofSeconds(10));
        List<WebElement> allArticles = wait.until(ExpectedConditions.refreshed(ExpectedConditions.presenceOfAllElementsLocatedBy(articleSelector)));
        return allArticles.subList(0, maxLength);
    }

    private static void downloadCoverImageIfAvailable(WebElement article, String imageTitle){
        String coverImagePath = "./child::figure/a/img";
        if (article.findElements(By.xpath(coverImagePath)).size() > 0) {
            WebElement articleImage = article.findElement(By.xpath(coverImagePath));
            URL articleImageURL = null;
            try {
                articleImageURL = new URL(articleImage.getAttribute("src"));
                BufferedImage saveImage = ImageIO.read(articleImageURL);
                ImageIO.write(saveImage, "jpg", new File("./images/" + imageTitle + ".jpg"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String translateSpanishTextToEnglish(String spanishText){
        System.out.println("**Sending Translation API Request**");
        String baseURL = "https://google-translate113.p.rapidapi.com/api/v1/translator/text";
        RequestSpecification request = RestAssured.given()
                .header("x-rapidapi-key", "2f5eb685f7mshacb65afa35bb6bep14aa11jsnd935b3716c4e")
                .header("x-rapidapi-host", "google-translate113.p.rapidapi.com")
                .header("Content-Type", "application/json");

        JSONObject requestParams = new JSONObject();
        requestParams.put("from", "es");
        requestParams.put("to", "en");
        requestParams.put("text", spanishText);

        request.body(requestParams.toString());
        Response response = request.post(baseURL);

        JsonPath jsonPath = response.jsonPath();
        String translatedTitle = jsonPath.get("trans").toString();
        System.out.println("Spanish Text: "+spanishText+" Translated text:" + translatedTitle);
        return translatedTitle;
    }

    private static void printDuplicateWordsInTitles(String[] translatedTitles){
        //Print each repeated word along with the count of its occurrences
        String combinedTitles = String.join(" ",translatedTitles);
        Map<String,Integer> wordCountMap=new HashMap<>();
        for (String word:combinedTitles.split(" ")){
            if (wordCountMap.containsKey(word)){

                wordCountMap.put(word,wordCountMap.get(word)+1);
            }else {
                wordCountMap.put(word,1);
            }
        }

        System.out.println("============ Repeated words across titles ============");
        wordCountMap.forEach((word,wordcount)->{
            if (wordcount>1) {
                System.out.println("Repeated Word: " + word + " , " + "Occurrence Count: " + wordcount);
            }
        });
    }

    private static String[] processArticlesAndGetTranslations(List<WebElement> articles){
        String[] titles = new String[articles.size()];
        String[] translatedTitles = new String[articles.size()];

        for (int i=0; i < articles.size();i++) {
            WebElement article = articles.get(i);
            String titlePath = "./child::*/h2/a";
            titles[i] = article.findElement(By.xpath(titlePath)).getText();
            System.out.println("Processing Article: " + (i + 1));
            System.out.println("Title: " + titles[i]);
            String content = article.findElement(By.xpath("./child::p")).getText();
            System.out.println("Content: " + content);

            // Get translated title
            String translatedText = translateSpanishTextToEnglish(titles[i]);
            System.out.println("Translated title: "+translatedText);
            translatedTitles[i] = translatedText;

            //Verify and Save Cover Image of each article, if available
            downloadCoverImageIfAvailable(article,"Article-"+(i+1));
        }

        return translatedTitles;
    }


}