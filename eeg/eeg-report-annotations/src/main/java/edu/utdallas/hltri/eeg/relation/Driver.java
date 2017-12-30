package edu.utdallas.hltri.eeg.relation;

import edu.utdallas.hltri.eeg.io.EegActivityBratCorpus;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.BratCorpus;

import java.io.File;
import java.util.Scanner;

/**
 * Created by stuart on 5/13/16.
 */
public class Driver {
    private static final Logger log = Logger.get(Driver.class);
    public static void main(String[] args)throws Exception{
        if(args.length == 0 || args[0].equals("--help")) {
            System.out.println("arguments for program are\njava whatever brat/corpus/dir/ rules/file/location");
            return;
        }if(args[0].equals("--prompt")){
            Scanner input = new Scanner(new File(args[1]));
            args = new String[2];
            System.out.println("Enter dir for brat corpus");
            args[0]=input.nextLine();
            System.out.println("Enter path for rules file");
            args[1]=input.nextLine();
            input.close();
        }
        EegActivityBratCorpus corpus = new EegActivityBratCorpus(new File(args[0]),"foo");
        BratCorpus.BratRules rfile = new BratCorpus.BratRules(args[1]);
        rfile.processCorpus(corpus,"foo");
        //*/
    }
}
