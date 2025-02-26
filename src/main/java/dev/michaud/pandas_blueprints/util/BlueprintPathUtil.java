package dev.michaud.pandas_blueprints.util;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Identifier;
import net.minecraft.util.PathUtil;

public class BlueprintPathUtil {

  private static final int MAX_FILENAME_LENGTH = 255;
  private static final Pattern FILE_NAME_WITH_COUNT = Pattern.compile("(?<name>.*) #(?<count>\\d+)",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Pattern RESERVED_WINDOWS_NAMES = Pattern.compile(
      ".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?",
      Pattern.CASE_INSENSITIVE);

  /**
   * Basically the same as {@link PathUtil#getNextUniqueName(Path, String, String)}, but with a
   * slightly changed format.
   *
   * @return A file name, prefixed with {@code name}, that does not currently exist within
   * {@code parentFolder}.
   *
   * @implNote This strips any illegal characters from {@code name}, then attempts to make a
   * directory with the name and the extension. If this succeeds, the directory is deleted and the
   * name with the extension is returned. If not, it appends {@code #2} to the name and tries again
   * until it succeeds.
   */
  public static String getNextUniqueName(Path parentFolder, String name, String extension) throws IOException {

    name = format(name);
    Matcher matcher = FILE_NAME_WITH_COUNT.matcher(name);

    int attempt = 0;

    // Extract existing count, if present
    if (matcher.matches()) {
      name = matcher.group("name");
      attempt = Integer.parseInt(matcher.group("count"));
    }

    // Truncate
    final int maxLength = MAX_FILENAME_LENGTH - extension.length();
    if (name.length() > maxLength) {
      name = name.substring(0, maxLength);
    }

    // Find new value
    while (true) {
      String candidate = countedName(name, extension, attempt);
      Path candidatePath = parentFolder.resolve(candidate + extension);

      try {
        Path createdDir = Files.createDirectory(candidatePath);
        Files.deleteIfExists(createdDir);
        return candidate;
      } catch (FileAlreadyExistsException e) {
        attempt++;
      }
    }
  }

  public static String countedName(String name, String extension, int number) {
    final String suffix = number != 0 ? "_" + number : "";
    final int maxLength = MAX_FILENAME_LENGTH - suffix.length() - extension.length();

    if (name.length() > maxLength) {
      name = name.substring(0, maxLength);
    }

    return name + suffix;
  }

  public static String format(String name) {

    name = PathUtil.replaceInvalidChars(name.toLowerCase())
        .replaceAll("(?![a-z0-9._-]).", "_");

    if (RESERVED_WINDOWS_NAMES.matcher(name).matches()) {
      name = "_" + name + "_";
    }

    return name;
  }

}