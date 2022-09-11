package de.bender.notes.boundary;

import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;

@CommandLine.Command(name = "completion",
        header = "bash/zsh completion:  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})",
        helpCommand = true)
public class Completion extends GenerateCompletion  {
}
