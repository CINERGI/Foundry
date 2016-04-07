package org.neuinfo.foundry.consumers.common;

import org.apache.commons.cli.*;

/**
 * Created by bozyurt on 4/7/16.
 */
public class UserManCLI {
    String configFile = "cinergi-consumers-cfg.xml";

    public UserManCLI(String configFile) {
        this.configFile = configFile;
    }

    public UserManCLI() {
    }

    void createUsers(String[] users) throws Exception {
        Helper helper = new Helper("");
        try {
            helper.startup(configFile);
            for(String userStr : users) {
                String username = userStr;
                String password = userStr;
                int idx = userStr.indexOf(':');
                if (idx != -1) {
                    username = userStr.substring(0, idx);
                    password = userStr.substring(idx+1);
                }
                System.out.println("creating user:" + username);
                helper.saveUser(username, password);
            }
        } finally {
            helper.shutdown();
        }
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ProvenanceUploader", options);
        System.exit(1);
    }


    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option configFileOption = OptionBuilder.withArgName("config-file")
                .hasArg().withDescription("config-file e.g. cinergi-consumers-cfg.xml").create('c');
        Option sourceIdOption = OptionBuilder.withArgName("user(s)")
                .hasArg().isRequired(true)
                .withDescription("A comma separated list of users (username[:pwd]").create('u');
        Options options = new Options();
        options.addOption(help);
        options.addOption(configFileOption);
        options.addOption(sourceIdOption);
        CommandLineParser cli = new GnuParser();
        CommandLine line = null;
        try {
            line = cli.parse(options, args);
        } catch (Exception x) {
            System.err.println(x.getMessage());
            usage(options);
        }
         if (line.hasOption("h") || !line.hasOption("u")) {
            usage(options);
        }
        UserManCLI umc = new UserManCLI();

        if (line.hasOption('c')) {
            String configFile = line.getOptionValue('c');
            umc.configFile = configFile;
        }
        String usersStr = line.getOptionValue('u');
        String[] users = usersStr.split("\\s*,\\s*");

        umc.createUsers(users);
    }
}
