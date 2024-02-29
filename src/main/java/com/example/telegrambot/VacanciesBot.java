package com.example.telegrambot;

import com.example.telegrambot.Service.VacancyService;
import com.example.telegrambot.dto.VacancyDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VacanciesBot extends TelegramLongPollingBot {
    @Autowired
    private VacancyService vacancyService;

    private final Map<Long, String> lastShownVacancyLevel = new HashMap<>();

    public VacanciesBot() {
        super("6031695208:AAFke51x0O9HOYBseVNtUpe8sGZPYrEDPb8");
    }

    @Override
    public void onUpdateReceived(Update update) {
      try {
          if (update.getMessage() != null) {
              handleStartCommand(update);
          }
          if (update.getCallbackQuery() != null) {
              String callbackData = update.getCallbackQuery().getData();

              if ("showJuniorVacancies".equals(callbackData)) {
                  showJuniorVacancies(update);
              } else if ("showMiddleVacancies".equals(callbackData)) {
                  showMiddleVacancies(update);
              } else if ("showSeniorVacancies".equals(callbackData)){
                  showSeniorVacancies(update);
              } else if (callbackData.startsWith("vacancyId=")){
                  String id = callbackData.split("=")[1];
                  showVacancyDescription(id, update);
              } else if ("backToVacancies".equals(callbackData)){
                  handleBackToVacanciesCommand(update);
              } else if ("backToStartMenu".equals(callbackData)){
                  handleBackToStartCommand(update);
              }
          }
      }
        catch (Exception e){
            throw new RuntimeException("Can't send message to user!", e);
          }
    }

    private void handleBackToVacanciesCommand(Update update) throws TelegramApiException {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String level = lastShownVacancyLevel.get(chatId);

        if("junior".equals(level)){
            showJuniorVacancies(update);
        } else if ("middle".equals(level)){
            showMiddleVacancies(update);
        } else if ("senior".equals(level)){
            showSeniorVacancies(update);
        }
    }

    private void handleBackToStartCommand(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Choose title:");
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setReplyMarkup(getStartMenu());
        execute(sendMessage);
    }

    private void showVacancyDescription(String id, Update update) throws TelegramApiException {
        VacancyDto vacancyDto = vacancyService.get(id);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        String vacancyInfo = """
                *Tittle:* %s
                *Company:* %s
                *Short description:* %s
                *Description:* %s
                *Salary:* %s
                *Link:* [%s](%s)
                """.formatted(
                        escapeMarkdownReserveChars(vacancyDto.getTitle()),
                    escapeMarkdownReserveChars(vacancyDto.getCompany()),
                    escapeMarkdownReserveChars(vacancyDto.getShortDescription()),
                    escapeMarkdownReserveChars(vacancyDto.getLongDescription()),
                    vacancyDto.getSalary().contains("-") ? "Not specified" : escapeMarkdownReserveChars(vacancyDto.getSalary()),
                    "Click here for more details",
                    escapeMarkdownReserveChars(vacancyDto.getLink())
        );
        sendMessage.setText(vacancyInfo);
        sendMessage.setParseMode(ParseMode.MARKDOWNV2);
        sendMessage.setReplyMarkup(getBackToVacanciesMenu());
        execute(sendMessage);
    }

    private String escapeMarkdownReserveChars(String text){
        return text.replace("-","\\-")
                .replace("_","\\_")
                .replace("*","\\*")
                .replace("[","\\[")
                .replace("]","\\]")
                .replace("(","\\(")
                .replace(")","\\)")
                .replace("~","\\~")
                .replace("`","\\`")
                .replace(">","\\>")
                .replace("#","\\#")
                .replace("+","\\+")
                .replace(".","\\.")
                .replace("!","\\!");

    }

    private ReplyKeyboard getBackToVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backToVacanciesButton = new InlineKeyboardButton();
        backToVacanciesButton.setText("Back to vacancies");
        backToVacanciesButton.setCallbackData("backToVacancies");
        row.add(backToVacanciesButton);

        InlineKeyboardButton backToStartMenuButton = new InlineKeyboardButton();
        backToStartMenuButton.setText("Back to start menu");
        backToStartMenuButton.setCallbackData("backToStartMenu");
        row.add(backToStartMenuButton);

        InlineKeyboardButton getChatGptButton = new InlineKeyboardButton();
        getChatGptButton.setText("Get cover letter");
        getChatGptButton.setUrl("https://chat.openai.com/");
        row.add(getChatGptButton);

        return new InlineKeyboardMarkup(List.of(row));
    }

    private ReplyKeyboard getJuniorVacanciesMenu() {
        List<VacancyDto> vacancies = vacancyService.getJuniorVacancies();

        return getVacanciesMenu(vacancies);
    }
    private ReplyKeyboard getMiddleVacancies(){
        List<VacancyDto> vacancies = vacancyService.getMiddleVacancies();

        return getVacanciesMenu(vacancies);
    }

    private ReplyKeyboard getSeniorVacancies(){
        List<VacancyDto> vacancies = vacancyService.getSeniorVacancies();

        return getVacanciesMenu(vacancies);
    }

    private ReplyKeyboard getVacanciesMenu(List<VacancyDto> vacancies){
        List<InlineKeyboardButton> row = new ArrayList<>();

        for(VacancyDto vacancy: vacancies){
            InlineKeyboardButton vacancyButton = new InlineKeyboardButton();
            vacancyButton.setText(vacancy.getTitle());
            vacancyButton.setCallbackData("vacancyId=" + vacancy.getId());
            row.add(vacancyButton);
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));
        return keyboard;
    }

    private void showJuniorVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Plese choose vacancy:");
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getJuniorVacanciesMenu());
        execute(sendMessage);

        lastShownVacancyLevel.put(chatId, "junior");
    }

    private void showMiddleVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please choose vacancy: ");
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getMiddleVacancies());
        execute(sendMessage);

        lastShownVacancyLevel.put(chatId, "middle");
    }

    private void showSeniorVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please choose vacancy: ");
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getSeniorVacancies());
        execute(sendMessage);

        lastShownVacancyLevel.put(chatId, "senior");
    }

    private void handleStartCommand(Update update){
        String text = update.getMessage().getText();
        System.out.println("Received text is " + text);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("Welcome to vacancies bot! Please, choose your title:");
        sendMessage.setReplyMarkup(getStartMenu());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    private ReplyKeyboard getStartMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton junior = new InlineKeyboardButton();
        junior.setText("Junior");
        junior.setCallbackData("showJuniorVacancies");
        row.add(junior);

        InlineKeyboardButton middle = new InlineKeyboardButton();
        middle.setText("Middle");
        middle.setCallbackData("showMiddleVacancies");
        row.add(middle);

        InlineKeyboardButton senior = new InlineKeyboardButton();
        senior.setText("Senior");
        senior.setCallbackData("showSeniorVacancies");
        row.add(senior);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));
        return keyboard;
    }

    @Override
    public String getBotUsername() {
        return "kortizol_vacancies_bot";
    }
}
