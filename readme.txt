=============================================================
   Osalausestaja: Clause Segmenter for Estonian
=============================================================

  Clause Segmenter is a program that splits long and complex natural 
 language sentences into smaller segments (clauses). For example, the 
 sentence "Mees, keda seal kohtasime, oli tuttav ja teretas meid." 
 will be split into following clauses:

     "[Mees, [keda seal kohtasime,] oli tuttav ja] [teretas meid.]"
     (in the example, clauses are surrounded by brackets)

   The algorithm mainly relies on punctuation, conjunction words, and
  finite verb forms on identifying the clause boundaries.
  For linguistic details/motivations behind the algorithm, see (Kaalep, 
  Muischnek 2012).

=========================
   Requirements
=========================
 For building the program (JAR file):
  ** Java JDK (at least version 1.8.x is expected);
  ** Apache Ant (at least version 1.8.2);
  
 For using the program:
  ** A sentence segmentator;
  ** A word tokenizer;
  ** Estonian morphological analyzer, possible options:
     -- Filosoft Vabamorf: https://github.com/Filosoft/vabamorf
     -- PyVabamorf:        https://github.com/estnltk/pyvabamorf
     -- T3MESTA (a commercial morphological analyzer);
  ** Estonian morphological disambiguator;
     -- Vabamorf's disambiguator: 
        https://github.com/Filosoft/vabamorf
     NB! The clause segmenter also works on morphologically ambiguous 
        input, but the quality of the analysis is expected to be lower 
        than in the case of morphologically disambiguated text.

=========================
   Building the program
=========================
   The most straightforward way for compiling the program is by using
  Apache Ant and the build script ("build.xml" in root dir);
  
   Before building, correct path to JDK must be set in the file 
  "build.properties" (variable "java.home.location"). Then, building
  and deploying can be evoked with the command:

      ant deploy

  (in the same directory where "build.xml" is located);
   This compiles the Java source code, makes the JAR file (Osalau.jar), 
  and copies the JAR file along with required files into the folder 
  "test";
  
=========================
   Using the program
=========================

    Basic usage
   ---------------
   Before the clause segmenter can be applied on a text, a number of 
  text preprocessing steps must be made: text must be split into 
  sentences and tokens (words), and words must be morphologically 
  analysed (and disambiguated).
   Core of these functionalities is provided by EstNLTK toolkit, so the 
  easiest way to use the clause segmenter is within this toolkit ( see 
  https://github.com/estnltk/estnltk   for more details ).

   It is expected that the input of the clause segmenter is in the same 
  format as the output of Vabamorf's command 'etana analyze' - a JSON 
  structured text in UTF8 encoding. Note that the clause segmenter expects 
  that word root analyses are 'clean', without any phonetic markup symbols 
  (which can be optionally added in 'etana' with flag '-phonetic').

  An example of JSON input can be found in file "test/example_input.json";
  In the "test" folder, following command evokes clause segmenter on
  the input file "example_input.json" and outputs the results to standard 
  input:
  
     java -jar Osalau.jar -in file example_input.json -pretty_print

  (flag "-pretty_print" switches on the pretty printing mode, otherwise, 
  all of the output JSON is on single line);

  Alternatively, output can also be directed to a file by specifying:

     java -jar Osalau.jar -in file example_input.json -pretty_print -out file my_output.json

  Flag "-pyvabamorf" evokes the program in a special standard input/output 
  processing mode, where the program reads a line from the standard input,
  analyzes the line, and outputs the results (in a single line) to the standard 
  output.

     java -jar Osalau.jar -pyvabamorf

   More details about Vabamorf and its JSON format:   
        https://github.com/Filosoft/vabamorf 


    The "insensitive to missing commas" mode
   -------------------------------------------
    The clause segmenter can also be executed in the mode in which the program 
   attempts to be less sensitive to missing commas while detecting clause boundaries. 
   
    The flag "-ins_comma_mis" can be used to switch this mode on:
    
     java -jar Osalau.jar -in file example_input_missing_commas.json -pretty_print -ins_comma_mis
   
    (the file "example_input_missing_commas.json" can be found in the folder "test");
    
     Note that this mode is experimental and compared to the default mode, it can 
   introduce additional incorrect clause boundaries, although it also improves clause 
   boundary detection in texts with (a lot of) missing commas.
   
============================
   Interpreting the output
============================

  The clause segmenter marks clause boundaries: boundaries between regular 
 clauses, and start and end positions of embedded clauses. 
 
  In JSON input/output format, the clause boundary is indicated by adding object 
 'clauseAnnotation' to the token (at the same level as objects 'text' and 
 'analysis'). The 'clauseAnnotation' (which is a list of strings) can contain 
 three types of boundary markings:
    KINDEL_PIIR -- indicates that there is a clause boundary AFTER current 
                   token: one clause ends and another starts;
    KIILU_ALGUS -- marks a beginning of a new embedded clause BEFORE current 
                   token;
    KIILU_LOPP  -- marks ending of an embedded clause AFTER current token;

  Example:
    The sentence
       "Mees, keda seal kohtasime, oli tuttav ja teretas meid."

    will obtain following clause annotations:
        {'words': [ {'analysis': [ ... ],
                      'text': 'Mees,'},
                     {'analysis': [ ... ],
                      'clauseAnnotation': ['KIILU_ALGUS'],
                      'text': 'keda'},
                     {'analysis': [ ... ],
                      'text': 'seal'},
                     {'analysis': [ ... ],
                      'clauseAnnotation': ['KIILU_LOPP'],
                      'text': 'kohtasime,'},
                     {'analysis': [ ... ],
                      'text': 'oli'},
                     {'analysis': [ ... ],
                      'text': 'tuttav'},
                     {'analysis': [ ... ],
                      'clauseAnnotation': ['KINDEL_PIIR'],
                      'text': 'ja'},
                     {'analysis': [ ... ],
                      'text': 'teretas'},
                     {'analysis': [ ... ],
                      'text': 'meid.'} ]}

    which should be interpreted as:
          "keda" (KIILU_ALGUS) -- an embedded clause begins before "keda";
          "kohtasime," (KIILU_LOPP) -- the embedded clause ends after "kohtasime,";
          "ja" (KINDEL_PIIR)   -- one clause ends after "ja" and another begins;
          
    so, the corresponding clause structure should look like:
       "[Mees, [keda seal kohtasime,] oli tuttav ja] [teretas meid.]"
       (clauses are surrounded by brackets)
       
  Note that embedded clauses can contain other clauses and other embedded 
 clauses, and so the whole clause structure has a recursive nature.

=========================
   References
=========================

  *) Kaalep, Heiki-Jaan; Muischnek, Kadri (2012). Osalausete tuvastamine 
     eestikeelses tekstis kui iseseisev ülesanne. Helle Metslang, Margit 
     Langemets, Maria-Maren Sepper (Toim.). Eesti Rakenduslingvistika Ühingu 
     aastaraamat (55 - 68). Tallinn: Eesti Rakenduslingvistika Ühing;

  *) Kaalep, Heiki-Jaan; Muischnek, Kadri (2012). Robust clause boundary 
     identification for corpus annotation. Nicoletta Calzolari, Khalid Choukri, 
     Thierry Declerck, Mehmet Uğur Doğan, Bente Maegaard, Joseph Mar (Toim.). 
     Proceedings of the Eight International Conference on Language Resources 
     and Evaluation (LREC'12) (1632 - 1636). Istanbul, Türgi: ELRA;
