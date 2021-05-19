package mainTest;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import controller.*;
import model.DurationModel;
import model.exceptions.ManySeparatorsException;
import model.exceptions.PathIsNotDefined;
import model.exceptions.SeparatorException;
import model.exceptions.SimilarPoints;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.mockito.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RequestTest {

    private final Request request = new Request();

    private final ShowContent showContent = new ShowContent();
    private final DurationModel durationModel = new DurationModel();
    private final DurationAPI durationAPI = new DurationAPI();
    private WebDriver driver = null;


    private static final String[] separators = new String[]{
            ">",
            "...",
            "~",
            " to ",
            " - "
    };


    private static final long MINUTE = 1;
    private static final long HOUR = 60;
    private static final long DAY = 60 * 24;

    public void checkMock(String telegramJson, String command, String response) throws IOException, TelegramApiException {

        telegramJson = telegramJson.replace("request", command);
        String BOT_TOKEN = "1729543646:AAHsnbZEAQGr97WhTVytqMYmXkUDjHsFrto";
        String BOT_USERNAME = "ImplemicaDirectionBot";

        Context context = new Context() {
            @Override
            public String getAwsRequestId() {
                return null;
            }

            @Override
            public String getLogGroupName() {
                return null;
            }

            @Override
            public String getLogStreamName() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }

            @Override
            public String getFunctionVersion() {
                return null;
            }

            @Override
            public String getInvokedFunctionArn() {
                return null;
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 0;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 0;
            }

            @Override
            public LambdaLogger getLogger() {
                return null;
            }
        };

        Bot bot = Mockito.spy(Bot.class);
        Request request = new Request(bot);

        SendMessage expectedSend = new SendMessage();
        expectedSend.enableMarkdown(true);
        expectedSend.setText(response);

        // actual objects


        InputStream actualStream = new ByteArrayInputStream(telegramJson.getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<SendMessage> argumentSent = ArgumentCaptor.forClass(SendMessage.class);

        request.handleRequest(actualStream, null, context);

        verify(bot).init(BOT_TOKEN, BOT_USERNAME);
        verify(bot).execute(argumentSent.capture());

        SendMessage actualSend = argumentSent.getValue();

        assertEquals(BOT_TOKEN, bot.getBot().getBotToken());
        assertEquals(BOT_USERNAME, bot.getBot().getBotUsername());
        assertEquals(expectedSend.getText(), actualSend.getText());
        assertEquals(expectedSend.getParseMode(), actualSend.getParseMode());


    }

    private void checkSelenium(String text, String expectedMessage) throws InterruptedException {

        String[] commands = text.split(" ");
        for(String command: commands) {
            driver.findElement(By.xpath("//*[@class=\"composer_rich_textarea\"]")).sendKeys(command);
            driver.findElement(By.xpath("//*[@type=\"submit\"]")).click();
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        }
        List<WebElement> webElementList = driver.findElements(By.xpath("//*[@class=\"im_message_text\"]"));
        String actualMessage = webElementList.get(webElementList.size() - 1).getText();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void testSelenium() throws InterruptedException {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\User\\Desktop\\SeleniumChromeDriver\\chromedriver90.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-data-dir=C:/Users/User/AppData/Local/Google/Chrome/User Data");
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        driver.get("https://web.telegram.org/");
        driver.findElement(By.xpath("//*[@class=\"im_dialogs_search\"]/input")).sendKeys("ImplemicaDirectionBot");
        driver.findElement(By.xpath("//*[@class=\"im_dialog\"]")).click();

        String HELP_MESSAGE = "I can help you calculate the time " + "\n" +
                "it takes to get from point A to point B." + "\n\n" +
                "You can control me by sending" + "\n" +
                "/duration" + "\n\n" +
                "After that you will have to enter " + "\n" +
                "your origin and destination " + "\n" +
                "according to the given rule:" + "\n\n" +
                "<street>,<house>,<city>,<region>,<country>" + "\n\n" +
                "For example:" + "\n" +
                "1) Paris > London" + "\n" +
                "2) Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine > Kyrpychova St, 2, Kharkiv, Kharkivs'ka Oblast, Ukraine";

        checkSelenium("/help", HELP_MESSAGE);
        checkSelenium("/any", HELP_MESSAGE);
        checkSelenium("asdglasdgjla", HELP_MESSAGE);
        checkSelenium("1234", HELP_MESSAGE);

        String DURATION_MASSAGE = "Please set origin and destination:";
        String SEPARATOR_EXCEPTION = "You need to enter a separator.";
        String MANY_SEPARATOR_EXCEPTION = "You only need to enter one separator.";
        String SIMILAR_POINTS_EXCEPTION = "You need to enter different travel points.";

        checkSelenium("/duration", DURATION_MASSAGE);
        checkSelenium("any", SEPARATOR_EXCEPTION);
        checkSelenium("/duration Paris>>London", MANY_SEPARATOR_EXCEPTION);
        checkSelenium("/duration Paris>Paris", SIMILAR_POINTS_EXCEPTION);

        driver.quit();

    }

    @Test
    public void testMock() throws IOException, TelegramApiException {
        String telegramSimple = ""
                + "{\n"
                + "    \"update_id\": 7.69695785E8,\n"
                + "    \"message\": {\n"
                + "        \"message_id\": 5558.0,\n"
                + "        \"from\": {\n"
                + "            \"id\": 5.69556267E8,\n"
                + "            \"is_bot\": false,\n"
                + "            \"first_name\": \"Хижняк\",\n"
                + "            \"last_name\": \"Саша\",\n"
                + "            \"language_code\": \"ru\"\n"
                + "        },\n"
                + "        \"chat\": {\n"
                + "            \"id\": 5.69556267E8,\n"
                + "            \"first_name\": \"Хижняк\",\n"
                + "            \"last_name\": \"Саша\",\n"
                + "            \"type\": \"private\"\n"
                + "        },\n"
                + "        \"date\": 1.620303115E9,\n"
                + "        \"text\": \"" + "request" + "\"\n"
                + "    }\n"
                + "}";

        String telegramReply = ""
                + "{\n"
                + "    \"update_id\": 7.69695785E8,\n"
                + "    \"message\": {\n"
                + "        \"message_id\": 5558.0,\n"
                + "        \"from\": {\n"
                + "            \"id\": 5.69556267E8,\n"
                + "            \"is_bot\": false,\n"
                + "            \"first_name\": \"Хижняк\",\n"
                + "            \"last_name\": \"Саша\",\n"
                + "            \"language_code\": \"ru\"\n"
                + "        },\n"
                + "        \"chat\": {\n"
                + "            \"id\": 5.69556267E8,\n"
                + "            \"first_name\": \"Хижняк\",\n"
                + "            \"first_name\": \"Саша\",\n"
                + "            \"type\": \"private\"\n"
                + "        },\n"
                + "        \"date\": 1.620303115E9,\n"
                + "        \"forward_from\": {\n"
                + "            \"id\": 1.729543646E9,\n"
                + "            \"is_bot\": false,\n"
                + "            \"first_name\": \"DurationBot\",\n"
                + "            \"username\": \"ImplemicaDirectionBot\"\n"
                + "        },\n"
                + "        \"forward_date\": 1.621244153E9,\n"
                + "        \"text\": \"" + "request" + "\"\n"
                + "    }\n"
                + "}";

        String telegramFile = ""
                + "{\n"
                + "    \"update_id\": 7.69695785E8,\n"
                + "    \"message\": {\n"
                + "        \"message_id\": 5558.0,\n"
                + "        \"from\": {\n"
                + "            \"id\": 5.69556267E8,\n"
                + "            \"is_bot\": false,\n"
                + "            \"first_name\": \"Хижняк\",\n"
                + "            \"last_name\": \"Саша\",\n"
                + "            \"language_code\": \"ru\"\n"
                + "        },\n"
                + "        \"chat\": {\n"
                + "            \"id\": 5.69556267E8,\n"
                + "            \"first_name\": \"Хижняк\",\n"
                + "            \"first_name\": \"Саша\",\n"
                + "            \"type\": \"private\"\n"
                + "        },\n"
                + "        \"date\": 1.620303115E9,\n"
                + "        \"document\": {\n"
                + "            \"file_name\": \"Filename.docx\",\n"
                + "            \"file_id\": \"BQACAgIAAxkBAAIWbWCiP3KoWdKGwZ4dLxzWBZn61g7KAAJ6DAACiMcYSTJ0VAbfuFuuHwQ.docx\",\n"
                + "            \"file_unique_id\": \"AgADegwAAojHGEk\",\n"
                + "            \"file_size\": \"2488872\"\n"
                + "        },\n"
                + "        \"text\": \"" + "request" + "\"\n"
                + "    }\n"
                + "}";

        String HELP_MESSAGE_MARKDOWN = "I can help you calculate the time " + "\n" +
                "it takes to get from point A to point B." + "\n\n" +
                "You can control me by sending" + "\n" +
                "/duration" + "\n\n" +
                "After that you will have to enter " + "\n" +
                "your *origin* and *destination* " + "\n" +
                "according to the given rule:" + "\n\n" +
                "<street>,<house>,<city>,<region>,<country>" + "\n\n" +
                "For example:" + "\n" +
                "_1) Paris > London_" + "\n" +
                "_2) Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine > Kyrpychova St, 2, Kharkiv, Kharkivs'ka Oblast, Ukraine_";

        checkMock(telegramSimple, "/help", HELP_MESSAGE_MARKDOWN);
        checkMock(telegramReply, "/help", HELP_MESSAGE_MARKDOWN);
        checkMock(telegramFile, "/help", HELP_MESSAGE_MARKDOWN);
    }

    private void checkMenuException(String text, String exceptionMessage) {
        request.menu("/duration");
        assertEquals(exceptionMessage, request.menu(text));
    }

    private String getDurationPartStr(long durationMode) {
        String res;

        if (durationMode != 0) {
            res = showContent.showPart(Duration.ofMinutes(durationMode));
        } else {
            res = "is not defined";
        }

        return res;
    }

    private Duration getDurationPart(long durationMode) {
        Duration duration = null;

        if (durationMode != 0) {
            duration = Duration.ofMinutes(durationMode);
        }

        return duration;
    }

    private String checkDurationCreate(long driving, long transit, long bicycling, long walking) {
        String actual = null;

        try {
            durationModel.createDuration(
                    getDurationPart(driving),
                    getDurationPart(transit),
                    getDurationPart(bicycling),
                    getDurationPart(walking)
            );
        } catch (PathIsNotDefined pathIsNotDefined) {
            actual = "Path is not defined.";
        }

        return actual;
    }

    private void checkDurationShow(String origin, String destination, long driving, long transit, long bicycling, long walking) {
        String actual;
        String expected = "The duration time:\n\n" +
                "from " + origin + " to " + destination + "\n\n" +
                "\uD83D\uDE97 driving - " + getDurationPartStr(driving) + "\n" +
                "\uD83D\uDE8C transit - " + getDurationPartStr(transit) + "\n" +
                "\uD83D\uDEB2 bicycling - " + getDurationPartStr(bicycling) + "\n" +
                "\uD83D\uDEB6 walking - " + getDurationPartStr(walking);

        if (driving == 0 && transit == 0 && bicycling == 0 && walking == 0) {
            expected = "Path is not defined.";
        }

        durationModel.setOrigin(origin);
        durationModel.setDestination(destination);

        String exceptionString = checkDurationCreate(driving, transit, bicycling, walking);

        if (exceptionString != null) {
            actual = exceptionString;
        } else {
            actual = showContent.showDuration(durationModel);
        }

        assertEquals(expected, actual);
    }

    public void checkGoogleJSONRead(String text, long actualMinutes) {
        JsonElement jsonElement = null;
        try {
            jsonElement = JsonParser.parseReader(new FileReader("src/test/java/mainTest/JSONgoogleSample"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String content = jsonElement.toString();
        String durationConstant = "text\"" + ":" + "\"";
        content = content.replace(durationConstant, durationConstant + text);
        long actual = actualMinutes;
        Duration duration = durationAPI.googleJSONRead(content);
        if (duration.toMinutes() == 0) {
            actual = 0;
        }
        assertEquals(actual, duration.toMinutes());
    }

    public void checkDurationShow(String text, long actualMinutes) {
        String actual;

        if (actualMinutes > 0) {
            actual = showContent.showPart(Duration.ofMinutes(actualMinutes));
        } else {
            actual = showContent.showPart(null);
        }
        assertEquals(text, actual);
    }

    public void checkDurationCreate(String text, long actualMinutes) {
        Duration duration = durationAPI.timeParse(text);
        long actual = actualMinutes;

        if (duration.toMinutes() == 0) {
            actual = 0;
        }

        assertEquals(actual, duration.toMinutes());
    }


    private void checkSeparator(String text, String expected) {
        durationModel.setSeparators(separators);
        String actual;

        try {
            actual = durationModel.findSeparator(text);
        } catch (SeparatorException e) {
            actual = "Separator is not defined";
        } catch (ManySeparatorsException e) {
            actual = "Many separators";
        }

        assertEquals(expected, actual);
    }

    private void checkPathPoints(String text, String expected) {
        durationModel.setSeparators(separators);

        String actual;
        try {
            durationModel.createPathPoints(text);
            String actualOrigin = durationModel.getOrigin();
            String actualDestination = durationModel.getDestination();

            actual = actualOrigin + " " + actualDestination;
        } catch (SimilarPoints similarPoints) {
            actual = "You have identified the same points.";
        } catch (SeparatorException e) {
            actual = "Separator is not defined";
        } catch (ManySeparatorsException e) {
            actual = "Many separators";
        }

        assertEquals(expected, actual);
    }

    public void checkPartHandling(String text, long actualMinutes) {
        checkDurationShow(text, actualMinutes);
        checkDurationCreate(text, actualMinutes);
        checkGoogleJSONRead(text, actualMinutes);
    }

    @Test
    public void menuCommand() {

        // 2. classes

        // CITIES

        // paris - london

        checkSeparator("Paris > London", ">");
        checkSeparator("Paris ... London", "...");
        checkSeparator("Paris ~ London", "~");
        checkSeparator("Paris to London", " to ");
        checkSeparator("Paris - London", " - ");

        checkPathPoints("Paris > London", "Paris London");
        checkPathPoints("Paris ... London", "Paris London");
        checkPathPoints("Paris ~ London", "Paris London");
        checkPathPoints("Paris to London", "Paris London");
        checkPathPoints("Paris - London", "Paris London");

        checkDurationShow("Paris", "London", 1, 1, 1, 1);
        checkDurationShow("Paris", "London", 2, 2, 2, 2);
        checkDurationShow("Paris", "London", 1, 2, 3, 4);
        checkDurationShow("Paris", "London", 4, 3, 2, 1);
        checkDurationShow("Paris", "London", 24, 145, 24, 123);
        checkDurationShow("Paris", "London", 24, 145, 24, 123);

        // kyiv - kharkiv

        checkSeparator("Kyiv > Kharkiv", ">");
        checkSeparator("Kyiv ... Kharkiv", "...");
        checkSeparator("Kyiv ~ Kharkiv", "~");
        checkSeparator("Kyiv to Kharkiv", " to ");
        checkSeparator("Kyiv - Kharkiv", " - ");

        checkPathPoints("Kyiv > Kharkiv", "Kyiv Kharkiv");
        checkPathPoints("Kyiv ... Kharkiv", "Kyiv Kharkiv");
        checkPathPoints("Kyiv ~ Kharkiv", "Kyiv Kharkiv");
        checkPathPoints("Kyiv to Kharkiv", "Kyiv Kharkiv");
        checkPathPoints("Kyiv - Kharkiv", "Kyiv Kharkiv");

        checkDurationShow("Kyiv", "Kharkiv", 1, 1, 1, 1);
        checkDurationShow("Kyiv", "Kharkiv", 2, 2, 2, 2);
        checkDurationShow("Kyiv", "Kharkiv", 1, 2, 3, 4);
        checkDurationShow("Kyiv", "Kharkiv", 4, 3, 2, 1);
        checkDurationShow("Kyiv", "Kharkiv", 24, 145, 24, 123);
        checkDurationShow("Kyiv", "Kharkiv", 24, 145, 24, 123);

        // moscow - saint-petersburg

        checkSeparator("Moscow > Saint-Petersburg", ">");
        checkSeparator("Moscow ... Saint-Petersburg", "...");
        checkSeparator("Moscow ~ Saint-Petersburg", "~");
        checkSeparator("Moscow to Saint-Petersburg", " to ");
        checkSeparator("Moscow - Saint-Petersburg", " - ");

        checkPathPoints("Moscow > Saint-Petersburg", "Moscow Saint-Petersburg");
        checkPathPoints("Moscow ... Saint-Petersburg", "Moscow Saint-Petersburg");
        checkPathPoints("Moscow ~ Saint-Petersburg", "Moscow Saint-Petersburg");
        checkPathPoints("Moscow to Saint-Petersburg", "Moscow Saint-Petersburg");
        checkPathPoints("Moscow - Saint-Petersburg", "Moscow Saint-Petersburg");

        checkDurationShow("Moscow", "Saint-Petersburg", 1, 1, 1, 1);
        checkDurationShow("Moscow", "Saint-Petersburg", 2, 2, 2, 2);
        checkDurationShow("Moscow", "Saint-Petersburg", 1, 2, 3, 4);
        checkDurationShow("Moscow", "Saint-Petersburg", 4, 3, 2, 1);
        checkDurationShow("Moscow", "Saint-Petersburg", 24, 145, 24, 123);
        checkDurationShow("Moscow", "Saint-Petersburg", 24, 145, 24, 123);

        // STREETS

        // Baker St, London, UK > Oxford St, London, UK

        checkSeparator("Baker St, London, UK > Oxford St, London, UK", ">");
        checkSeparator("Baker St, London, UK ... Oxford St, London, UK", "...");
        checkSeparator("Baker St, London, UK ~ Oxford St, London, UK", "~");
        checkSeparator("Baker St, London, UK to Oxford St, London, UK", " to ");
        checkSeparator("Baker St, London, UK - Oxford St, London, UK", " - ");

        checkPathPoints("Baker St, London, UK > Oxford St, London, UK", "Baker St, London, UK Oxford St, London, UK");
        checkPathPoints("Baker St, London, UK ... Oxford St, London, UK", "Baker St, London, UK Oxford St, London, UK");
        checkPathPoints("Baker St, London, UK ~ Oxford St, London, UK", "Baker St, London, UK Oxford St, London, UK");
        checkPathPoints("Baker St, London, UK to Oxford St, London, UK", "Baker St, London, UK Oxford St, London, UK");
        checkPathPoints("Baker St, London, UK - Oxford St, London, UK", "Baker St, London, UK Oxford St, London, UK");

        checkDurationShow("Baker St, London, UK", "Oxford St, London, UK", 1, 1, 1, 1);
        checkDurationShow("Baker St, London, UK", "Oxford St, London, UK", 2, 2, 2, 2);
        checkDurationShow("Baker St, London, UK", "Oxford St, London, UK", 1, 2, 3, 4);
        checkDurationShow("Baker St, London, UK", "Oxford St, London, UK", 4, 3, 2, 1);
        checkDurationShow("Baker St, London, UK", "Oxford St, London, UK", 24, 145, 24, 123);
        checkDurationShow("Baker St, London, UK", "Oxford St, London, UK", 24, 145, 24, 123);

        // Tverskaya St, Moskva, Russia > Nikolskaya St, Moskva, Russia

        checkSeparator("Tverskaya St, Moskva, Russia > Nikolskaya St, Moskva, Russia", ">");
        checkSeparator("Tverskaya St, Moskva, Russia ... Nikolskaya St, Moskva, Russia", "...");
        checkSeparator("Tverskaya St, Moskva, Russia ~ Nikolskaya St, Moskva, Russia", "~");
        checkSeparator("Tverskaya St, Moskva, Russia to Nikolskaya St, Moskva, Russia", " to ");
        checkSeparator("Tverskaya St, Moskva, Russia - Nikolskaya St, Moskva, Russia", " - ");

        checkPathPoints("Tverskaya St, Moskva, Russia > Nikolskaya St, Moskva, Russia", "Tverskaya St, Moskva, Russia Nikolskaya St, Moskva, Russia");
        checkPathPoints("Tverskaya St, Moskva, Russia ... Nikolskaya St, Moskva, Russia", "Tverskaya St, Moskva, Russia Nikolskaya St, Moskva, Russia");
        checkPathPoints("Tverskaya St, Moskva, Russia ~ Nikolskaya St, Moskva, Russia", "Tverskaya St, Moskva, Russia Nikolskaya St, Moskva, Russia");
        checkPathPoints("Tverskaya St, Moskva, Russia to Nikolskaya St, Moskva, Russia", "Tverskaya St, Moskva, Russia Nikolskaya St, Moskva, Russia");
        checkPathPoints("Tverskaya St, Moskva, Russia - Nikolskaya St, Moskva, Russia", "Tverskaya St, Moskva, Russia Nikolskaya St, Moskva, Russia");

        checkDurationShow("Tverskaya St, Moskva, Russia", "Nikolskaya St, Moskva, Russia", 1, 1, 1, 1);
        checkDurationShow("Tverskaya St, Moskva, Russia", "Nikolskaya St, Moskva, Russia", 2, 2, 2, 2);
        checkDurationShow("Tverskaya St, Moskva, Russia", "Nikolskaya St, Moskva, Russia", 1, 2, 3, 4);
        checkDurationShow("Tverskaya St, Moskva, Russia", "Nikolskaya St, Moskva, Russia", 4, 3, 2, 1);
        checkDurationShow("Tverskaya St, Moskva, Russia", "Nikolskaya St, Moskva, Russia", 24, 145, 24, 123);
        checkDurationShow("Tverskaya St, Moskva, Russia", "Nikolskaya St, Moskva, Russia", 24, 145, 24, 123);

        // Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine > Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine

        checkSeparator("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine > Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", ">");
        checkSeparator("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ... Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", "...");
        checkSeparator("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ~ Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", "~");
        checkSeparator("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine to Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", " to ");
        checkSeparator("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine - Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", " - ");

        checkPathPoints("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine  > Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine  ... Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine  ~ Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine  to Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine  - Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine");

        checkDurationShow("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", 1, 1, 1, 1);
        checkDurationShow("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", 2, 2, 2, 2);
        checkDurationShow("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", 1, 2, 3, 4);
        checkDurationShow("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", 4, 3, 2, 1);
        checkDurationShow("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", 24, 145, 24, 123);
        checkDurationShow("Pavlivs'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Pushkins'ka St, Kharkiv, Kharkivs'ka oblast, Ukraine", 24, 145, 24, 123);

        //HOUSES

        // 33-34 B524, London, UK > woodmart, 451 Wall St, London, United Kingdom

        checkSeparator("33-34 B524, London, UK > woodmart, 451 Wall St, London, United Kingdom", ">");
        checkSeparator("33-34 B524, London, UK ... woodmart, 451 Wall St, London, United Kingdom", "...");
        checkSeparator("33-34 B524, London, UK ~ woodmart, 451 Wall St, London, United Kingdom", "~");
        checkSeparator("33-34 B524, London, UK to woodmart, 451 Wall St, London, United Kingdom", " to ");
        checkSeparator("33-34 B524, London, UK - woodmart, 451 Wall St, London, United Kingdom", " - ");

        checkPathPoints("33-34 B524, London, UK > woodmart, 451 Wall St, London, United Kingdom", "33-34 B524, London, UK woodmart, 451 Wall St, London, United Kingdom");
        checkPathPoints("33-34 B524, London, UK ... woodmart, 451 Wall St, London, United Kingdom", "33-34 B524, London, UK woodmart, 451 Wall St, London, United Kingdom");
        checkPathPoints("33-34 B524, London, UK ~ woodmart, 451 Wall St, London, United Kingdom", "33-34 B524, London, UK woodmart, 451 Wall St, London, United Kingdom");
        checkPathPoints("33-34 B524, London, UK to woodmart, 451 Wall St, London, United Kingdom", "33-34 B524, London, UK woodmart, 451 Wall St, London, United Kingdom");
        checkPathPoints("33-34 B524, London, UK - woodmart, 451 Wall St, London, United Kingdom", "33-34 B524, London, UK woodmart, 451 Wall St, London, United Kingdom");

        checkDurationShow("33-34 B524, London, UK", "woodmart, 451 Wall St, London, United Kingdom", 1, 1, 1, 1);
        checkDurationShow("33-34 B524, London, UK", "woodmart, 451 Wall St, London, United Kingdom", 2, 2, 2, 2);
        checkDurationShow("33-34 B524, London, UK", "woodmart, 451 Wall St, London, United Kingdom", 1, 2, 3, 4);
        checkDurationShow("33-34 B524, London, UK", "woodmart, 451 Wall St, London, United Kingdom", 4, 3, 2, 1);
        checkDurationShow("33-34 B524, London, UK", "woodmart, 451 Wall St, London, United Kingdom", 24, 145, 24, 123);
        checkDurationShow("33-34 B524, London, UK", "woodmart, 451 Wall St, London, United Kingdom", 24, 145, 24, 123);


        //Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine > Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine

        checkSeparator("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  > Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", ">");
        checkSeparator("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  ... Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", "...");
        checkSeparator("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  ~ Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", "~");
        checkSeparator("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  to Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", " to ");
        checkSeparator("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  - Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", " - ");

        checkPathPoints("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  > Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", "Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine");
        checkPathPoints("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  ... Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", "Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine");
        checkPathPoints("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  ~ Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", "Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine");
        checkPathPoints("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  to Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", "Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine");
        checkPathPoints("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine  - Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", "Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine");

        checkDurationShow("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine ", "Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", 1, 1, 1, 1);
        checkDurationShow("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine ", "Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", 2, 2, 2, 2);
        checkDurationShow("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine ", "Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", 1, 2, 3, 4);
        checkDurationShow("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine ", "Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", 4, 3, 2, 1);
        checkDurationShow("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine ", "Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", 24, 145, 24, 123);
        checkDurationShow("Kyrpychova St, 2, Kharkiv, Kharkiv Oblast, Ukraine ", "Akademika Kurchatova Ave, 1-A, Kharkiv, Kharkiv Oblast, Ukraine", 24, 145, 24, 123);

        // Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine > Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine

        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  > Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", ">");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  ... Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", "...");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  ~ Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", "~");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  to Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", " to ");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  - Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", " - ");

        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  > Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  ... Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  ~ Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  to Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine  - Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine");

        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", 1, 1, 1, 1);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", 2, 2, 2, 2);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", 1, 2, 3, 4);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", 4, 3, 2, 1);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", 24, 145, 24, 123);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine ", "Yuvilejnyj Ave, 94, Kharkiv, Kharkivs'ka oblast, Ukraine", 24, 145, 24, 123);


        // 3. realization

        checkMenuException("ParisLondon", "You need to enter a separator.");
        checkMenuException("Paris London", "You need to enter a separator.");
        checkMenuException("ParistoLondon", "You need to enter a separator.");
        checkMenuException("Paris-London", "You need to enter a separator.");

        checkMenuException("Paris > > London", "You only need to enter one separator.");
        checkMenuException("Paris > ~ London", "You only need to enter one separator.");
        checkMenuException("Paris ... ~ London", "You only need to enter one separator.");
        checkMenuException("Paris > London >", "You only need to enter one separator.");
        checkMenuException("> Paris > London", "You only need to enter one separator.");
        checkMenuException("> Paris > London >", "You only need to enter one separator.");

        checkMenuException("Paris > Paris", "You need to enter different travel points.");
        checkMenuException("Kyiv > Kyiv", "You need to enter different travel points.");
        checkMenuException("Paris ... Paris", "You need to enter different travel points.");
        checkMenuException("Kyiv ... Kyiv", "You need to enter different travel points.");

        checkSeparator("ParisLondon", "Separator is not defined");
        checkSeparator("Paris London", "Separator is not defined");
        checkSeparator("ParistoLondon", "Separator is not defined");
        checkSeparator("Paris-London", "Separator is not defined");

        checkSeparator("Paris > > London", "Many separators");
        checkSeparator("Paris > ~ London", "Many separators");
        checkSeparator("Paris ... ~ London", "Many separators");
        checkSeparator("Paris > London >", "Many separators");
        checkSeparator("> Paris > London", "Many separators");
        checkSeparator("> Paris > London >", "Many separators");

        checkPathPoints("Paris > Paris", "You have identified the same points.");

        checkDurationShow("Paris", "London", 0, 0, 0, 0);

        // 5. random

        // Langensoultzbach, 67360, France > Le Vésinet, 78110, France

        checkSeparator("Langensoultzbach, 67360, France  > Le Vésinet, 78110, France", ">");
        checkSeparator("Langensoultzbach, 67360, France  ... Le Vésinet, 78110, France", "...");
        checkSeparator("Langensoultzbach, 67360, France  ~ Le Vésinet, 78110, France", "~");
        checkSeparator("Langensoultzbach, 67360, France  to Le Vésinet, 78110, France", " to ");
        checkSeparator("Langensoultzbach, 67360, France  - Le Vésinet, 78110, France", " - ");

        checkPathPoints("Langensoultzbach, 67360, France  > Le Vésinet, 78110, France", "Langensoultzbach, 67360, France Le Vésinet, 78110, France");
        checkPathPoints("Langensoultzbach, 67360, France  ... Le Vésinet, 78110, France", "Langensoultzbach, 67360, France Le Vésinet, 78110, France");
        checkPathPoints("Langensoultzbach, 67360, France  ~ Le Vésinet, 78110, France", "Langensoultzbach, 67360, France Le Vésinet, 78110, France");
        checkPathPoints("Langensoultzbach, 67360, France  to Le Vésinet, 78110, France", "Langensoultzbach, 67360, France Le Vésinet, 78110, France");
        checkPathPoints("Langensoultzbach, 67360, France  - Le Vésinet, 78110, France", "Langensoultzbach, 67360, France Le Vésinet, 78110, France");

        checkDurationShow("Langensoultzbach, 67360, France ", "Le Vésinet, 78110, France", 1, 1, 1, 1);
        checkDurationShow("Langensoultzbach, 67360, France ", "Le Vésinet, 78110, France", 2, 2, 2, 2);
        checkDurationShow("Langensoultzbach, 67360, France ", "Le Vésinet, 78110, France", 1, 2, 3, 4);
        checkDurationShow("Langensoultzbach, 67360, France ", "Le Vésinet, 78110, France", 4, 3, 2, 1);
        checkDurationShow("Langensoultzbach, 67360, France ", "Le Vésinet, 78110, France", 24, 145, 24, 123);
        checkDurationShow("Langensoultzbach, 67360, France ", "Le Vésinet, 78110, France", 24, 145, 24, 123);

        // Hügel 2, 98739 Lichte, Germany > Vasvári Pál u., Szigetvár, 7900 Hungary

        checkSeparator("Hügel 2, 98739 Lichte, Germany > Vasvári Pál u., Szigetvár, 7900 Hungary", ">");
        checkSeparator("Hügel 2, 98739 Lichte, Germany ... Vasvári Pál u., Szigetvár, 7900 Hungary", "...");
        checkSeparator("Hügel 2, 98739 Lichte, Germany ~ Vasvári Pál u., Szigetvár, 7900 Hungary", "~");
        checkSeparator("Hügel 2, 98739 Lichte, Germany to Vasvári Pál u., Szigetvár, 7900 Hungary", " to ");
        checkSeparator("Hügel 2, 98739 Lichte, Germany - Vasvári Pál u., Szigetvár, 7900 Hungary", " - ");

        checkPathPoints("Hügel 2, 98739 Lichte, Germany > Vasvári Pál u., Szigetvár, 7900 Hungary", "Hügel 2, 98739 Lichte, Germany Vasvári Pál u., Szigetvár, 7900 Hungary");
        checkPathPoints("Hügel 2, 98739 Lichte, Germany ... Vasvári Pál u., Szigetvár, 7900 Hungary", "Hügel 2, 98739 Lichte, Germany Vasvári Pál u., Szigetvár, 7900 Hungary");
        checkPathPoints("Hügel 2, 98739 Lichte, Germany ~ Vasvári Pál u., Szigetvár, 7900 Hungary", "Hügel 2, 98739 Lichte, Germany Vasvári Pál u., Szigetvár, 7900 Hungary");
        checkPathPoints("Hügel 2, 98739 Lichte, Germany to Vasvári Pál u., Szigetvár, 7900 Hungary", "Hügel 2, 98739 Lichte, Germany Vasvári Pál u., Szigetvár, 7900 Hungary");
        checkPathPoints("Hügel 2, 98739 Lichte, Germany - Vasvári Pál u., Szigetvár, 7900 Hungary", "Hügel 2, 98739 Lichte, Germany Vasvári Pál u., Szigetvár, 7900 Hungary");

        checkDurationShow("Hügel 2, 98739 Lichte, Germany", "Vasvári Pál u., Szigetvár, 7900 Hungary", 1, 1, 1, 1);
        checkDurationShow("Hügel 2, 98739 Lichte, Germany", "Vasvári Pál u., Szigetvár, 7900 Hungary", 2, 2, 2, 2);
        checkDurationShow("Hügel 2, 98739 Lichte, Germany", "Vasvári Pál u., Szigetvár, 7900 Hungary", 1, 2, 3, 4);
        checkDurationShow("Hügel 2, 98739 Lichte, Germany", "Vasvári Pál u., Szigetvár, 7900 Hungary", 4, 3, 2, 1);
        checkDurationShow("Hügel 2, 98739 Lichte, Germany", "Vasvári Pál u., Szigetvár, 7900 Hungary", 24, 145, 24, 123);
        checkDurationShow("Hügel 2, 98739 Lichte, Germany", "Vasvári Pál u., Szigetvár, 7900 Hungary", 24, 145, 24, 123);

        // 6. special

        //Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast, Ukraine > Paris

        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast > Paris", ">");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast ... Paris", "...");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast ~ Paris", "~");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast to Paris", " to ");
        checkSeparator("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast - Paris", " - ");

        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast > Paris", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast Paris");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast ... Paris", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast Paris");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast ~ Paris", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast Paris");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast to Paris", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast Paris");
        checkPathPoints("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast - Paris", "Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast Paris");

        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast", "Paris", 1, 1, 1, 1);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast", "Paris", 2, 2, 2, 2);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast", "Paris", 1, 2, 3, 4);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast", "Paris", 4, 3, 2, 1);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast", "Paris", 24, 145, 24, 123);
        checkDurationShow("Pavlivs'ka St, 6, Kharkiv, Kharkivs'ka oblast", "Paris", 24, 145, 24, 123);

        //Tverskaya St, Moskva, Russia > London

        checkSeparator("Tverskaya St, Moskva > London", ">");
        checkSeparator("Tverskaya St, Moskva ... London", "...");
        checkSeparator("Tverskaya St, Moskva ~ London", "~");
        checkSeparator("Tverskaya St, Moskva to London", " to ");
        checkSeparator("Tverskaya St, Moskva - London", " - ");

        checkPathPoints("Tverskaya St, Moskva > London", "Tverskaya St, Moskva London");
        checkPathPoints("Tverskaya St, Moskva ... London", "Tverskaya St, Moskva London");
        checkPathPoints("Tverskaya St, Moskva ~ London", "Tverskaya St, Moskva London");
        checkPathPoints("Tverskaya St, Moskva to London", "Tverskaya St, Moskva London");
        checkPathPoints("Tverskaya St, Moskva - London", "Tverskaya St, Moskva London");

        checkDurationShow("Tverskaya St, Moskva", "London", 1, 1, 1, 1);
        checkDurationShow("Tverskaya St, Moskva", "London", 2, 2, 2, 2);
        checkDurationShow("Tverskaya St, Moskva", "London", 1, 2, 3, 4);
        checkDurationShow("Tverskaya St, Moskva", "London", 4, 3, 2, 1);
        checkDurationShow("Tverskaya St, Moskva", "London", 24, 145, 24, 123);
        checkDurationShow("Tverskaya St, Moskva", "London", 24, 145, 24, 123);

        //Baker St, London, UK > Kharkiv

        checkSeparator("Baker St, London, UK  > Kharkiv", ">");
        checkSeparator("Baker St, London, UK  ... Kharkiv", "...");
        checkSeparator("Baker St, London, UK  ~ Kharkiv", "~");
        checkSeparator("Baker St, London, UK  to Kharkiv", " to ");
        checkSeparator("Baker St, London, UK  - Kharkiv", " - ");

        checkPathPoints("Baker St, London, UK  > Kharkiv", "Baker St, London, UK Kharkiv");
        checkPathPoints("Baker St, London, UK  ... Kharkiv", "Baker St, London, UK Kharkiv");
        checkPathPoints("Baker St, London, UK  ~ Kharkiv", "Baker St, London, UK Kharkiv");
        checkPathPoints("Baker St, London, UK  to Kharkiv", "Baker St, London, UK Kharkiv");
        checkPathPoints("Baker St, London, UK  - Kharkiv", "Baker St, London, UK Kharkiv");

        checkDurationShow("Baker St, London, UK ", "Kharkiv", 1, 1, 1, 1);
        checkDurationShow("Baker St, London, UK ", "Kharkiv", 2, 2, 2, 2);
        checkDurationShow("Baker St, London, UK ", "Kharkiv", 1, 2, 3, 4);
        checkDurationShow("Baker St, London, UK ", "Kharkiv", 4, 3, 2, 1);
        checkDurationShow("Baker St, London, UK ", "Kharkiv", 24, 145, 24, 123);
        checkDurationShow("Baker St, London, UK ", "Kharkiv", 24, 145, 24, 123);



    }

    @Test
    public void partHandling() {

        //2. classes

        // MINUTES

        // boundary-left
        checkPartHandling("is not defined", 0);
        checkPartHandling("1 min ", MINUTE);
        checkPartHandling("2 mins ", MINUTE * 2);

        // boundary-right
        checkPartHandling("58 mins ", MINUTE * 58);
        checkPartHandling("59 mins ", MINUTE * 59);
        checkPartHandling("1 hour ", MINUTE * 60);

        // inside

        checkPartHandling("6 mins ", MINUTE * 6);
        checkPartHandling("11 mins ", MINUTE * 11);
        checkPartHandling("23 mins ", MINUTE * 23);
        checkPartHandling("34 mins ", MINUTE * 34);
        checkPartHandling("47 mins ", MINUTE * 47);

        // HOURS

        // boundary-left
        checkPartHandling("59 mins ", MINUTE * 59);
        checkPartHandling("1 hour ", HOUR);
        checkPartHandling("2 hours ", HOUR * 2);

        // boundary-right
        checkPartHandling("22 hours ", HOUR * 22);
        checkPartHandling("23 hours ", HOUR * 23);
        checkPartHandling("1 day ", HOUR * 24);

        // inside

        checkPartHandling("5 hours ", HOUR * 5);
        checkPartHandling("9 hours ", HOUR * 9);
        checkPartHandling("11 hours ", HOUR * 11);
        checkPartHandling("13 hours ", HOUR * 13);
        checkPartHandling("20 hours ", HOUR * 20);

        // DAYS

        // boundary-left
        checkPartHandling("23 hours ", HOUR * 23);
        checkPartHandling("1 day ", DAY);
        checkPartHandling("2 days ", DAY * 2);

        // boundary-right
        checkPartHandling("365 days ", DAY * 365);
        checkPartHandling("366 days ", DAY * 366);
        checkPartHandling("is not defined", DAY * 367);

        // inside

        checkPartHandling("43 days ", DAY * 43);
        checkPartHandling("56 days ", DAY * 56);
        checkPartHandling("123 days ", DAY * 123);
        checkPartHandling("298 days ", DAY * 298);
        checkPartHandling("325 days ", DAY * 325);

        // COMBINE

        // DAY + HOUR

        // boundary-left

        checkPartHandling("1 day 1 hour ", DAY + HOUR);
        checkPartHandling("1 day 2 hours ", DAY + HOUR * 2);
        checkPartHandling("2 days 1 hour ", DAY * 2 + HOUR);
        checkPartHandling("2 days 2 hours ", DAY * 2 + HOUR * 2);

        // boundary-right

        checkPartHandling("366 days 23 hours ", DAY * 366 + HOUR * 23);
        checkPartHandling("is not defined", DAY * 366 + HOUR * 24);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 23);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 24);

        // inside

        checkPartHandling("53 days 4 hours ", DAY * 53 + HOUR * 4);
        checkPartHandling("66 days 7 hours ", DAY * 66 + HOUR * 7);
        checkPartHandling("98 days 11 hours ", DAY * 98 + HOUR * 11);
        checkPartHandling("225 days 14 hours ", DAY * 225 + HOUR * 14);
        checkPartHandling("323 days 20 hours ", DAY * 323 + HOUR * 20);


        // DAY + MIN

        // boundary-left

        checkPartHandling("1 day 1 min ", DAY + MINUTE);
        checkPartHandling("1 day 2 mins ", DAY + MINUTE * 2);
        checkPartHandling("2 days 1 min ", DAY * 2 + MINUTE);
        checkPartHandling("2 days 2 mins ", DAY * 2 + MINUTE * 2);

        // boundary-right

        checkPartHandling("366 days 59 mins ", DAY * 366 + MINUTE * 59);
        checkPartHandling("366 days 1 hour ", DAY * 366 + MINUTE * 60);
        checkPartHandling("is not defined", DAY * 367 + MINUTE * 59);
        checkPartHandling("is not defined", DAY * 367 + MINUTE * 60);

        // inside

        checkPartHandling("53 days 4 mins ", DAY * 53 + MINUTE * 4);
        checkPartHandling("66 days 17 mins ", DAY * 66 + MINUTE * 17);
        checkPartHandling("98 days 24 mins ", DAY * 98 + MINUTE * 24);
        checkPartHandling("225 days 45 mins ", DAY * 225 + MINUTE * 45);
        checkPartHandling("323 days 51 mins ", DAY * 323 + MINUTE * 51);


        // HOUR + MIN

        // boundary-left

        checkPartHandling("1 hour 1 min ", HOUR + MINUTE);
        checkPartHandling("1 hour 2 mins ", HOUR + MINUTE * 2);
        checkPartHandling("2 hours 1 min ", HOUR * 2 + MINUTE);
        checkPartHandling("2 hours 2 mins ", HOUR * 2 + MINUTE * 2);

        // boundary-right

        checkPartHandling("23 hours 59 mins ", HOUR * 23 + MINUTE * 59);
        checkPartHandling("1 day ", HOUR * 23 + MINUTE * 60);
        checkPartHandling("1 day 59 mins ", HOUR * 24 + MINUTE * 59);
        checkPartHandling("1 day 1 hour ", HOUR * 24 + MINUTE * 60);

        // inside

        checkPartHandling("4 hours 4 mins ", HOUR * 4 + MINUTE * 4);
        checkPartHandling("6 hours 17 mins ", HOUR * 6 + MINUTE * 17);
        checkPartHandling("9 hours 24 mins ", HOUR * 9 + MINUTE * 24);
        checkPartHandling("13 hours 45 mins ", HOUR * 13 + MINUTE * 45);
        checkPartHandling("18 hours 51 mins ", HOUR * 18 + MINUTE * 51);

        // DAY + HOUR + MIN

        // boundary-left

        checkPartHandling("1 day 1 hour 1 min ", DAY + HOUR + MINUTE);
        checkPartHandling("1 day 2 hours 1 min ", DAY + HOUR * 2 + MINUTE);
        checkPartHandling("2 days 1 hour 1 min ", DAY * 2 + HOUR + MINUTE);
        checkPartHandling("2 days 2 hours 1 min ", DAY * 2 + HOUR * 2 + MINUTE);

        checkPartHandling("1 day 1 hour 2 mins ", DAY + HOUR + MINUTE * 2);
        checkPartHandling("1 day 2 hours 2 mins ", DAY + HOUR * 2 + MINUTE * 2);
        checkPartHandling("2 days 1 hour 2 mins ", DAY * 2 + HOUR + MINUTE * 2);
        checkPartHandling("2 days 2 hours 2 mins ", DAY * 2 + HOUR * 2 + MINUTE * 2);

        checkPartHandling("1 day 1 hour 59 mins ", DAY + HOUR + MINUTE * 59);
        checkPartHandling("1 day 2 hours 59 mins ", DAY + HOUR * 2 + MINUTE * 59);
        checkPartHandling("2 days 1 hour 59 mins ", DAY * 2 + HOUR + MINUTE * 59);
        checkPartHandling("2 days 2 hours 59 mins ", DAY * 2 + HOUR * 2 + MINUTE * 59);

        checkPartHandling("1 day 2 hours ", DAY + HOUR + MINUTE * 60);
        checkPartHandling("1 day 3 hours ", DAY + HOUR * 2 + MINUTE * 60);
        checkPartHandling("2 days 2 hours ", DAY * 2 + HOUR + MINUTE * 60);
        checkPartHandling("2 days 3 hours ", DAY * 2 + HOUR * 2 + MINUTE * 60);


        // boundary-right

        checkPartHandling("366 days 23 hours 1 min ", DAY * 366 + HOUR * 23 + MINUTE);
        checkPartHandling("is not defined", DAY * 366 + HOUR * 24 + MINUTE);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 23 + MINUTE);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 24 + MINUTE);

        checkPartHandling("366 days 23 hours 2 mins ", DAY * 366 + HOUR * 23 + MINUTE * 2);
        checkPartHandling("is not defined", DAY * 366 + HOUR * 24 + MINUTE * 2);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 23 + MINUTE * 2);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 24 + MINUTE * 2);

        checkPartHandling("366 days 23 hours 59 mins ", DAY * 366 + HOUR * 23 + MINUTE * 59);
        checkPartHandling("is not defined", DAY * 366 + HOUR * 24 + MINUTE * 59);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 23 + MINUTE * 59);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 24 + MINUTE * 59);

        checkPartHandling("is not defined", DAY * 366 + HOUR * 23 + MINUTE * 60);
        checkPartHandling("is not defined", DAY * 366 + HOUR * 24 + MINUTE * 60);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 23 + MINUTE * 60);
        checkPartHandling("is not defined", DAY * 367 + HOUR * 24 + MINUTE * 60);

        // inside

        checkPartHandling("53 days 4 hours 1 min ", DAY * 53 + HOUR * 4 + MINUTE);
        checkPartHandling("66 days 7 hours 1 min ", DAY * 66 + HOUR * 7 + MINUTE);
        checkPartHandling("98 days 11 hours 1 min ", DAY * 98 + HOUR * 11 + MINUTE);
        checkPartHandling("225 days 14 hours 1 min ", DAY * 225 + HOUR * 14 + MINUTE);
        checkPartHandling("323 days 20 hours 1 min ", DAY * 323 + HOUR * 20 + MINUTE);

        checkPartHandling("53 days 4 hours 2 mins ", DAY * 53 + HOUR * 4 + MINUTE * 2);
        checkPartHandling("66 days 7 hours 2 mins ", DAY * 66 + HOUR * 7 + MINUTE * 2);
        checkPartHandling("98 days 11 hours 2 mins ", DAY * 98 + HOUR * 11 + MINUTE * 2);
        checkPartHandling("225 days 14 hours 2 mins ", DAY * 225 + HOUR * 14 + MINUTE * 2);
        checkPartHandling("323 days 20 hours 2 mins ", DAY * 323 + HOUR * 20 + MINUTE * 2);

        checkPartHandling("53 days 4 hours 59 mins ", DAY * 53 + HOUR * 4 + MINUTE * 59);
        checkPartHandling("66 days 7 hours 59 mins ", DAY * 66 + HOUR * 7 + MINUTE * 59);
        checkPartHandling("98 days 11 hours 59 mins ", DAY * 98 + HOUR * 11 + MINUTE * 59);
        checkPartHandling("225 days 14 hours 59 mins ", DAY * 225 + HOUR * 14 + MINUTE * 59);
        checkPartHandling("323 days 20 hours 59 mins ", DAY * 323 + HOUR * 20 + MINUTE * 59);

        checkPartHandling("53 days 5 hours ", DAY * 53 + HOUR * 4 + MINUTE * 60);
        checkPartHandling("66 days 8 hours ", DAY * 66 + HOUR * 7 + MINUTE * 60);
        checkPartHandling("98 days 12 hours ", DAY * 98 + HOUR * 11 + MINUTE * 60);
        checkPartHandling("225 days 15 hours ", DAY * 225 + HOUR * 14 + MINUTE * 60);
        checkPartHandling("323 days 21 hours ", DAY * 323 + HOUR * 20 + MINUTE * 60);

        //3. realization

        checkPartHandling("is not defined", 0);
        checkPartHandling("is not defined", -1);
        checkPartHandling("is not defined", -2);
        checkPartHandling("is not defined", -3);
        checkPartHandling("is not defined", -1000);
        checkPartHandling("is not defined", DAY * 367);
        checkPartHandling("is not defined", DAY * 368);
        checkPartHandling("is not defined", DAY * 1000);
        checkPartHandling("is not defined", DAY * 346733477);

        //4. subject area

        // entire

        checkPartHandling("1 hour ", HOUR);
        checkPartHandling("2 hours ", HOUR * 2);
        checkPartHandling("3 hours ", HOUR * 3);
        checkPartHandling("4 hours ", HOUR * 4);
        checkPartHandling("5 hours ", HOUR * 5);
        checkPartHandling("6 hours ", HOUR * 6);
        checkPartHandling("7 hours ", HOUR * 7);
        checkPartHandling("8 hours ", HOUR * 8);
        checkPartHandling("9 hours ", HOUR * 9);
        checkPartHandling("10 hours ", HOUR * 10);
        checkPartHandling("11 hours ", HOUR * 11);
        checkPartHandling("12 hours ", HOUR * 12);
        checkPartHandling("13 hours ", HOUR * 13);
        checkPartHandling("14 hours ", HOUR * 14);
        checkPartHandling("15 hours ", HOUR * 15);
        checkPartHandling("16 hours ", HOUR * 16);
        checkPartHandling("17 hours ", HOUR * 17);
        checkPartHandling("18 hours ", HOUR * 18);
        checkPartHandling("19 hours ", HOUR * 19);
        checkPartHandling("20 hours ", HOUR * 20);
        checkPartHandling("21 hours ", HOUR * 21);
        checkPartHandling("22 hours ", HOUR * 22);
        checkPartHandling("23 hours ", HOUR * 23);

        // half

        checkPartHandling("1 hour 30 mins ", HOUR + MINUTE * 30);
        checkPartHandling("2 hours 30 mins ", HOUR * 2 + MINUTE * 30);
        checkPartHandling("3 hours 30 mins ", HOUR * 3 + MINUTE * 30);
        checkPartHandling("4 hours 30 mins ", HOUR * 4 + MINUTE * 30);
        checkPartHandling("5 hours 30 mins ", HOUR * 5 + MINUTE * 30);
        checkPartHandling("6 hours 30 mins ", HOUR * 6 + MINUTE * 30);
        checkPartHandling("7 hours 30 mins ", HOUR * 7 + MINUTE * 30);
        checkPartHandling("8 hours 30 mins ", HOUR * 8 + MINUTE * 30);
        checkPartHandling("9 hours 30 mins ", HOUR * 9 + MINUTE * 30);
        checkPartHandling("10 hours 30 mins ", HOUR * 10 + MINUTE * 30);
        checkPartHandling("11 hours 30 mins ", HOUR * 11 + MINUTE * 30);
        checkPartHandling("12 hours 30 mins ", HOUR * 12 + MINUTE * 30);
        checkPartHandling("13 hours 30 mins ", HOUR * 13 + MINUTE * 30);
        checkPartHandling("14 hours 30 mins ", HOUR * 14 + MINUTE * 30);
        checkPartHandling("15 hours 30 mins ", HOUR * 15 + MINUTE * 30);
        checkPartHandling("16 hours 30 mins ", HOUR * 16 + MINUTE * 30);
        checkPartHandling("17 hours 30 mins ", HOUR * 17 + MINUTE * 30);
        checkPartHandling("18 hours 30 mins ", HOUR * 18 + MINUTE * 30);
        checkPartHandling("19 hours 30 mins ", HOUR * 19 + MINUTE * 30);
        checkPartHandling("20 hours 30 mins ", HOUR * 20 + MINUTE * 30);
        checkPartHandling("21 hours 30 mins ", HOUR * 21 + MINUTE * 30);
        checkPartHandling("22 hours 30 mins ", HOUR * 22 + MINUTE * 30);
        checkPartHandling("23 hours 30 mins ", HOUR * 23 + MINUTE * 30);

        // quater to

        checkPartHandling("45 mins ", HOUR - MINUTE * 15);
        checkPartHandling("1 hour 45 mins ", HOUR * 2 - MINUTE * 15);
        checkPartHandling("2 hours 45 mins ", HOUR * 3 - MINUTE * 15);
        checkPartHandling("3 hours 45 mins ", HOUR * 4 - MINUTE * 15);
        checkPartHandling("4 hours 45 mins ", HOUR * 5 - MINUTE * 15);
        checkPartHandling("5 hours 45 mins ", HOUR * 6 - MINUTE * 15);
        checkPartHandling("6 hours 45 mins ", HOUR * 7 - MINUTE * 15);
        checkPartHandling("7 hours 45 mins ", HOUR * 8 - MINUTE * 15);
        checkPartHandling("8 hours 45 mins ", HOUR * 9 - MINUTE * 15);
        checkPartHandling("9 hours 45 mins ", HOUR * 10 - MINUTE * 15);
        checkPartHandling("10 hours 45 mins ", HOUR * 11 - MINUTE * 15);
        checkPartHandling("11 hours 45 mins ", HOUR * 12 - MINUTE * 15);
        checkPartHandling("12 hours 45 mins ", HOUR * 13 - MINUTE * 15);
        checkPartHandling("13 hours 45 mins ", HOUR * 14 - MINUTE * 15);
        checkPartHandling("14 hours 45 mins ", HOUR * 15 - MINUTE * 15);
        checkPartHandling("15 hours 45 mins ", HOUR * 16 - MINUTE * 15);
        checkPartHandling("16 hours 45 mins ", HOUR * 17 - MINUTE * 15);
        checkPartHandling("17 hours 45 mins ", HOUR * 18 - MINUTE * 15);
        checkPartHandling("18 hours 45 mins ", HOUR * 19 - MINUTE * 15);
        checkPartHandling("19 hours 45 mins ", HOUR * 20 - MINUTE * 15);
        checkPartHandling("20 hours 45 mins ", HOUR * 21 - MINUTE * 15);
        checkPartHandling("21 hours 45 mins ", HOUR * 22 - MINUTE * 15);
        checkPartHandling("22 hours 45 mins ", HOUR * 23 - MINUTE * 15);
        checkPartHandling("23 hours 45 mins ", HOUR * 24 - MINUTE * 15);

        // quater past

        checkPartHandling("1 hour 15 mins ", HOUR + MINUTE * 15);
        checkPartHandling("2 hours 15 mins ", HOUR * 2 + MINUTE * 15);
        checkPartHandling("3 hours 15 mins ", HOUR * 3 + MINUTE * 15);
        checkPartHandling("4 hours 15 mins ", HOUR * 4 + MINUTE * 15);
        checkPartHandling("5 hours 15 mins ", HOUR * 5 + MINUTE * 15);
        checkPartHandling("6 hours 15 mins ", HOUR * 6 + MINUTE * 15);
        checkPartHandling("7 hours 15 mins ", HOUR * 7 + MINUTE * 15);
        checkPartHandling("8 hours 15 mins ", HOUR * 8 + MINUTE * 15);
        checkPartHandling("9 hours 15 mins ", HOUR * 9 + MINUTE * 15);
        checkPartHandling("10 hours 15 mins ", HOUR * 10 + MINUTE * 15);
        checkPartHandling("11 hours 15 mins ", HOUR * 11 + MINUTE * 15);
        checkPartHandling("12 hours 15 mins ", HOUR * 12 + MINUTE * 15);
        checkPartHandling("13 hours 15 mins ", HOUR * 13 + MINUTE * 15);
        checkPartHandling("14 hours 15 mins ", HOUR * 14 + MINUTE * 15);
        checkPartHandling("15 hours 15 mins ", HOUR * 15 + MINUTE * 15);
        checkPartHandling("16 hours 15 mins ", HOUR * 16 + MINUTE * 15);
        checkPartHandling("17 hours 15 mins ", HOUR * 17 + MINUTE * 15);
        checkPartHandling("18 hours 15 mins ", HOUR * 18 + MINUTE * 15);
        checkPartHandling("19 hours 15 mins ", HOUR * 19 + MINUTE * 15);
        checkPartHandling("20 hours 15 mins ", HOUR * 20 + MINUTE * 15);
        checkPartHandling("21 hours 15 mins ", HOUR * 21 + MINUTE * 15);
        checkPartHandling("22 hours 15 mins ", HOUR * 22 + MINUTE * 15);
        checkPartHandling("23 hours 15 mins ", HOUR * 23 + MINUTE * 15);

        // 5. random

        checkPartHandling("45 days 2 hours 34 mins ", DAY * 45 + HOUR * 2 + MINUTE * 34);
        checkPartHandling("11 days 6 hours 11 mins ", DAY * 11 + HOUR * 6 + MINUTE * 11);
        checkPartHandling("23 days 8 hours 23 mins ", DAY * 23 + HOUR * 8 + MINUTE * 23);
        checkPartHandling("is not defined", DAY * 8787 + HOUR * 2 + MINUTE * 42);
        checkPartHandling("14 hours 11 mins ", HOUR * 14 + MINUTE * 11);
        checkPartHandling("2 days 12 hours 22 mins ", DAY * 2 + HOUR * 12 + MINUTE * 22);
        checkPartHandling("11 days 21 hours 35 mins ", DAY * 11 + HOUR * 21 + MINUTE * 35);
        checkPartHandling("is not defined", DAY * 567 + HOUR * 8 + MINUTE * 40);
        checkPartHandling("2 days 5 hours 20 mins ", DAY * 2 + HOUR * 5 + MINUTE * 20);
        checkPartHandling("54 days 5 hours 15 mins ", DAY * 54 + HOUR * 5 + MINUTE * 15);
        checkPartHandling("76 days 7 hours 20 mins ", DAY * 76 + HOUR * 7 + MINUTE * 20);
        checkPartHandling("16 days 9 hours 54 mins ", DAY * 16 + HOUR * 9 + MINUTE * 54);
        checkPartHandling("74 days 11 hours 23 mins ", DAY * 74 + HOUR * 11 + MINUTE * 23);
        checkPartHandling("23 days 16 hours 32 mins ", DAY * 23 + HOUR * 16 + MINUTE * 32);
        checkPartHandling("14 days 12 hours 12 mins ", DAY * 14 + HOUR * 12 + MINUTE * 12);
        checkPartHandling("86 days 8 hours 6 mins ", DAY * 86 + HOUR * 8 + MINUTE * 6);
        checkPartHandling("111 days 23 hours 2 mins ", DAY * 111 + HOUR * 23 + MINUTE * 2);

        // 6. special

        checkPartHandling("1 day 1 hour 1 min ", DAY + HOUR + MINUTE);
        checkPartHandling("11 days 11 hours 11 mins ", DAY * 11 + HOUR * 11 + MINUTE * 11);
        checkPartHandling("2 days 2 hours 2 mins ", DAY * 2 + HOUR * 2 + MINUTE * 2);
        checkPartHandling("22 days 22 hours 22 mins ", DAY * 22 + HOUR * 22 + MINUTE * 22);
        checkPartHandling("3 days 3 hours 3 mins ", DAY * 3 + HOUR * 3 + MINUTE * 3);
        checkPartHandling("4 days 4 hours 4 mins ", DAY * 4 + HOUR * 4 + MINUTE * 4);
        checkPartHandling("5 days 5 hours 5 mins ", DAY * 5 + HOUR * 5 + MINUTE * 5);
        checkPartHandling("6 days 6 hours 6 mins ", DAY * 6 + HOUR * 6 + MINUTE * 6);
        checkPartHandling("7 days 7 hours 7 mins ", DAY * 7 + HOUR * 7 + MINUTE * 7);
        checkPartHandling("8 days 8 hours 8 mins ", DAY * 8 + HOUR * 8 + MINUTE * 8);
        checkPartHandling("9 days 9 hours 9 mins ", DAY * 9 + HOUR * 9 + MINUTE * 9);
        checkPartHandling("10 days 10 hours 10 mins ", DAY * 10 + HOUR * 10 + MINUTE * 10);

    }

}