package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.jooby.internal.SetHeaderImpl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Utility class for HTTP responses. Usually you start with a result builder {@link Results} and
 * then you customize (or not) one or more HTTP attribute.
 *
 * <p>
 * The following examples build the same output:
 * </p>
 *
 * <pre>
 * {
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.status(200).send("Something");
 *   });
 *
 *   get("/", req {@literal ->} Results.with("Something", 200);
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.5.0
 * @see Results
 */
public class Result {

  /** Response headers. */
  private Map<String, String> headers = new LinkedHashMap<>();

  /** Response header setter. */
  private SetHeaderImpl setHeader = new SetHeaderImpl((name, value) -> headers.put(name, value));

  /** Response status. */
  private Status status;

  /** Response content-type. */
  private MediaType type;

  private final Map<MediaType, Supplier<Object>> data = new LinkedHashMap<>();

  /**
   * Set response status.
   *
   * @param status A new response status to use.
   * @return This content.
   */
  public @Nonnull Result status(final @Nonnull Status status) {
    this.status = requireNonNull(status, "A status is required.");
    return this;
  }

  /**
   * Set response status.
   *
   * @param status A new response status to use.
   * @return This content.
   */
  public @Nonnull Result status(final int status) {
    return status(Status.valueOf(status));
  }

  /**
   * Set the content type of this content.
   *
   * @param type A content type.
   * @return This content.
   */
  public @Nonnull Result type(final @Nonnull MediaType type) {
    this.type = requireNonNull(type, "A content type is required.");
    return this;
  }

  /**
   * Set the content type of this content.
   *
   * @param type A content type.
   * @return This content.
   */
  public @Nonnull Result type(final @Nonnull String type) {
    return type(MediaType.valueOf(type));
  }

  /**
   * Set result content.
   *
   * @param content A result content.
   * @return This content.
   */
  public @Nonnull Result set(final @Nonnull Object content) {
    requireNonNull(content, "Content is required.");
    data.put(MediaType.all, () -> content);
    return this;
  }

  /**
   * Add a when clause for a custom result for the given media-type.
   *
   * @param type A media type to test for.
   * @param supplier An object supplier.
   * @return This result.
   */
  public @Nonnull Result when(final String type, final @Nonnull Supplier<Object> supplier) {
    return when(MediaType.valueOf(type), supplier);
  }

  /**
   * Add a when clause for a custom result for the given media-type.
   *
   * @param type A media type to test for.
   * @param supplier An object supplier.
   * @return This result.
   */
  public @Nonnull Result when(final MediaType type, @Nonnull final Supplier<Object> supplier) {
    requireNonNull(type, "A media type is required.");
    requireNonNull(supplier, "A supplier fn is required.");
    data.put(type, supplier);
    return this;
  }

  /**
   * @return True if result has content.
   */
  public boolean hasContent() {
    return data.size() > 0;
  }

  /**
   * @return Raw headers for content.
   */
  public @Nonnull Map<String, String> headers() {
    return ImmutableMap.copyOf(headers);
  }

  /**
   * @return Body status.
   */
  public @Nonnull Optional<Status> status() {
    return Optional.ofNullable(status);
  }

  /**
   * @return Body type.
   */
  public @Nonnull Optional<MediaType> type() {
    return Optional.ofNullable(type);
  }

  public @Nonnull Optional<Object> get() {
    return get(MediaType.ALL);
  }

  /**
   * @return Result content.
   */
  public @Nonnull Optional<Object> get(final List<MediaType> types) {
    requireNonNull(types, "Types are required.");
    int size = data.size();
    if (size == 1) {
      return Optional.of(data.values().iterator().next().get());
    }
    if (size == 0) {
      return Optional.empty();
    }
    Supplier<Object> provider = MediaType
        .matcher(types)
        .first(ImmutableList.copyOf(data.keySet()))
        .map(it -> data.get(it))
        .orElseThrow(
            () -> new Err(Status.NOT_ACCEPTABLE, Joiner.on(", ").join(types))
        );
    return Optional.of(provider.get());
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final char value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final byte value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final short value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final int value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public Result header(final String name, final long value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final float value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final double value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final CharSequence value) {
    setHeader.header(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public @Nonnull Result header(final @Nonnull String name, final @Nonnull Date value) {
    setHeader.header(name, value);
    return this;
  }

}