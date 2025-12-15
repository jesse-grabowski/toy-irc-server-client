package com.jessegrabowski.irc.args;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ArgsParserBuilder<T extends ArgsProperties> {
  ArgsParserBuilder<T> addUsageExample(String usageExample);

  ArgsParserBuilder<T> addBooleanFlag(
      char shortKey,
      String longKey,
      BiConsumer<T, Boolean> propertiesSetter,
      String description,
      boolean required);

  ArgsParserBuilder<T> addStringFlag(
      char shortKey,
      String longKey,
      BiConsumer<T, String> propertiesSetter,
      String description,
      boolean required);

  ArgsParserBuilder<T> addIntegerFlag(
      char shortKey,
      String longKey,
      BiConsumer<T, Integer> propertiesSetter,
      String description,
      boolean required);

  ArgsParserBuilder<T> addCharsetFlag(
      char shortKey,
      String longKey,
      BiConsumer<T, Charset> propertiesSetter,
      String description,
      boolean required);

  ArgsParserBuilder<T> addStringPositional(
      int position, BiConsumer<T, String> propertiesSetter, String description, boolean required);

  ArgsParserBuilder<T> addInetAddressPositional(
      int position,
      BiConsumer<T, InetAddress> propertiesSetter,
      String description,
      boolean required);

  ArgsParserBuilder<T> addCommaSeparatedListPositional(
      int position,
      BiConsumer<T, List<String>> propertiesSetter,
      String description,
      boolean required);

  ArgsParserBuilder<T> addGreedyStringPositional(
      int position, BiConsumer<T, String> propertiesSetter, String description, boolean required);

  ArgsParserBuilder<T> addGreedyListPositional(
      int position,
      Function<T, List<String>> propertiesGetter,
      BiConsumer<T, List<String>> propertiesSetter,
      String description,
      boolean required);

  ArgsParserBuilder<T> setFlagParsingEnabled(boolean value);

  ArgsParser<T> build();
}
