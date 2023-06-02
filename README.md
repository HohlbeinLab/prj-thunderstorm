# ThunderSTORM Optimized by Quantori

## Links
* [Issue created on the suggested improvement opportunities](https://github.com/zitmen/thunderstorm/issues/73)

## Description
ThunderSTORM is a popular open-source software tool that provides a set of tools for image processing, analysis, and visualization in single-molecule localization microscopy. To improve its functionality, we updated and optimized ThunderSTORM by focusing on enhancing its mathematical functions. Our goal was to increase its processing speed while maintaining a high degree of precision. The updates involved improving a general math module and optimizing particular fitting methods used in the software. These modifications significantly accelerated the performance of ThunderSTORM, and demonstrated that even minor modifications can significantly enhance the speed of the legacy software without sacrificing its accuracy.

## List of Changes
Three major updates were made to the original ThunderSTORM, with a focus on improving its mathematical functions:

* Enhancement of a general math module that was present in ThunderSTORM's source code by utilizing [Jafama](https://github.com/jeffhain/jafama): a Java library aiming at providing faster versions of java.lang.Math treatments.
* Update of an existing deprecated function used for calculating the Levenberg-Marquardt algorithm with a newer and more efficient implementation offered by [Apache Commons Math library version 3.6.1](https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/index.html).
* Substitution of a custom implementation of Nelder-Mead algorithm with Apache's relatively new solution from its [Apache Commons Math library version 3.6.1](https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/index.html) and adjustments of the parameters for the optimizer to further enhance the performance of the analysis.

The modifications were implemented based on [this development version](https://github.com/zitmen/thunderstorm/releases/tag/dev-2016-09-10-b1).

## Getting started
Install [ImageJ](http://imagej.nih.gov/ij/index.html) and download the latest version of [ThunderSTORM](https://github.com/quantori/prj-thunderstorm/releases/latest). For installation, copy the downloaded file into ImageJ's plugin subdirectory and run ImageJ. See the [Installation guide](https://github.com/zitmen/thunderstorm/wiki/Installation) for more information. To get started using ThunderSTORM, see the [Tutorials](https://github.com/zitmen/thunderstorm/wiki/Tutorials). Example data are provided [here](https://github.com/zitmen/thunderstorm/releases/download/v1.0/example_data.zip).
