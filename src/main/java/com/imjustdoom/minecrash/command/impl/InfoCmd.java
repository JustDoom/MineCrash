package com.imjustdoom.minecrash.command.impl;

import com.imjustdoom.minecrash.MineCrash;
import com.imjustdoom.minecrash.command.Command;
import com.imjustdoom.minecrash.util.CrashUtil;
import com.imjustdoom.minecrash.util.NetworkUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.util.List;

public class InfoCmd implements Command {

    @Override
    public String getName() {
        return "statistics";
    }

    @Override
    public String getDescription() {
        return "Displays statistics for the bot/project";
    }

    @Override
    public OptionData[] getOptions() {
        return new OptionData[0];
    }

    @Override
    public String[] getRoles() {
        return new String[0];
    }

    @Override
    public String[] getUsers() {
        return new String[0];
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        event.deferReply().queue();

        long guilds = MineCrash.get().getJda().getGuildCache().size();

        try {
            int[] values = NetworkUtil.getStatistics();
            event.getHook().sendMessageEmbeds(CrashUtil.getDefaultEmbed()
                    .setTitle("MineCrash Info")
                    .addField("Servers", String.valueOf(guilds), false)
                    .addField("Solved errors", String.valueOf(values[0]), false)
                    .addField("Errors for review", String.valueOf(values[1]), false)
                    .build()).queue();
        } catch (IOException exception) {
            event.getHook().sendMessageEmbeds(CrashUtil.getErrorEmbed().build()).queue();
        }
    }

    @Override
    public List<Command> getCommands() {
        return null;
    }
}
