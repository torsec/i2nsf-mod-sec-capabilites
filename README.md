# A model of capabilities of Network SecurityFunctions

Welcome to the GitHub repository containing the source code of the paper _A model of capabilities of Network
SecurityFunctions_ by C. Basile, D. Canavese, L. Regano, I. Pedone and A. Lioy. The source code is written in Java, but
we also provide the full project for Modelio (https://www.modelio.org/), a widely used open-source modeling environment.

This repository consists of these five folders:

- the `ConverterXMItoXSD` folder contains a tool that transform an XMI file into an XMLSchema file (this is used to
  generate the XMLSchema files of the Capability Information Model and the Capability Data Model);
- tbe `LanguageModelGenerator` folder contains an application that produces an XMLSchema file describing the language of
  a NSF;
- the `Modelio` folder contains the full Modelio project for the Capability Information Model and the Capability Data
  Model;
- the `PolicyRuleTranslator` folder contains a translator that generates a text file containing the low-level
  configuration rule for a NSF starting from a high-level policy; 
- finally, the  `Validation` folder contains a tool that performs the validation of an XML file with respect to an
  XMLSchema file.
  