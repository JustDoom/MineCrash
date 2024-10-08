package com.imjustdoom.minecrash.command.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.imjustdoom.minecrash.command.Command;
import com.imjustdoom.minecrash.exception.ErrorResponseException;
import com.imjustdoom.minecrash.exception.HttpConnectException;
import com.imjustdoom.minecrash.util.EmbedUtil;
import com.imjustdoom.minecrash.util.NetworkUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CrashCmd implements Command {

    @Override
    public String getName() {
        return "error";
    }

    @Override
    public String getDescription() {
        return "Tries to solve the error/crash";
    }

    @Override
    public OptionData[] getOptions() {
        return new OptionData[]{new OptionData(OptionType.ATTACHMENT, "error", "The file containing the error", false), new OptionData(OptionType.STRING, "errortext", "Text containing a short error", false)};
    }

    @Override
    public InteractionContextType[] getContexts() {
        return new InteractionContextType[]{InteractionContextType.GUILD, InteractionContextType.PRIVATE_CHANNEL, InteractionContextType.BOT_DM};
    }

    @Override
    public IntegrationType[] getTypes() {
        return new IntegrationType[]{IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL};
    }

    @Override
    public String[] getRoles() {
        return new String[]{};
    }

    @Override
    public String[] getUsers() {
        return new String[]{};
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        event.deferReply().queue();

        OptionMapping fileOption = event.getOption("error");
        OptionMapping textOption = event.getOption("errortext");

        if (fileOption != null) {
            Message.Attachment errorFile = fileOption.getAsAttachment();
            // Convert to MiB
            if ((errorFile.getSize()) > 12 * 1024 * 1024) {
                event.getHook().sendMessageEmbeds(EmbedUtil.getErrorEmbed().setDescription("File is too large, currently only 24MiB and below are supported").build()).queue();
                return;
            }

            if (errorFile.getContentType() == null || !errorFile.getContentType().contains("text/plain")) {
                event.getHook().sendMessageEmbeds(EmbedUtil.getErrorEmbed().setDescription("Failed to read file. Please upload a .txt file containing your error").build()).queue();
                return;
            }

            if (errorFile.getSize() > 1024f * 1024f) {
                System.out.println("File size is " + (errorFile.getSize() / 1024f / 1024f) + "MiB");
            } else if (errorFile.getSize() > 1024f) {
                System.out.println("File size is " + (errorFile.getSize() / 1024f) + "KiB");
            } else {
                System.out.println("File size is " + errorFile.getSize() + " bytes");
            }

            CompletableFuture<InputStream> futureFile = errorFile.getProxy().download();
            futureFile.whenComplete((inputStream, e) -> {
                String text = "";
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder out = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(System.lineSeparator()).append(line);
                    }
                    inputStream.close();
                    text = out.toString();
                } catch (IOException exception) {
                    event.getHook().sendMessage("Error").queue();
                    return;
                }

                checkError(event, text);
            });

            return;
        }

        if (textOption != null) {
            String error = textOption.getAsString();

            if (textOption.getAsString().isBlank()) {
                event.getHook().sendMessageEmbeds(EmbedUtil.getErrorEmbed().setDescription("Error is blank, please try again with an error specified.").build()).queue();
                return;
            }

            checkError(event, error);

            return;
        }

        event.getHook().sendMessageEmbeds(EmbedUtil.getDefaultEmbed().setTitle("Please specify either an error file or an error through text using the \"errortext\" option.").build()).queue();
    }

    private void checkError(SlashCommandInteractionEvent event, String error) {
        try {
            JsonElement json = NetworkUtil.sendPost(NetworkUtil.CHECK, error);
            if (!json.isJsonObject()) throw new IOException("Was expecting a JSON Object");

            JsonObject object = json.getAsJsonObject();
            if (object.has("solution") && object.has("title")) {
                event.getHook().sendMessageEmbeds(EmbedUtil.getDefaultEmbed()
                        .setTitle(object.get("title").getAsString())
                        .setDescription(object.get("solution").getAsString())
                        .setFooter(EmbedUtil.prependOnFooter("Took " + object.get("took") + "ms"))
                        .setColor(Color.GREEN)
                        .build()).queue();
            } else if (object.has("response")) {
                event.getHook().sendMessageEmbeds(EmbedUtil.getDefaultEmbed()
                        .setTitle("Unknown Crash/Error")
                        .setDescription(object.get("response").getAsString())
                        .setFooter(EmbedUtil.prependOnFooter("Took " + object.get("took") + "ms"))
                        .setColor(Color.ORANGE)
                        .build()).queue();
            }
        } catch (ErrorResponseException exception) {
            event.getHook().sendMessageEmbeds(EmbedUtil.getErrorEmbed().setDescription(exception.getError()).build()).queue();
        } catch (HttpConnectException exception) {
            event.getHook().sendMessageEmbeds(EmbedUtil.getErrorEmbed().setDescription("Was unable to connect to the endpoint...").build()).queue();
        } catch (IOException ex) {
            event.getHook().sendMessageEmbeds(EmbedUtil.getErrorEmbed().build()).queue();
            System.err.println("There was an error fetching or reading a response: " + ex.getMessage());
        }
    }

    @Override
    public List<Command> getCommands() {
        return null;
    }
}
