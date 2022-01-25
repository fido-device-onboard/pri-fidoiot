package org.fidoalliance.fdo.protocol.message;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"name", "more", "done", "mtu", "guid", "extra"})
public class ServiceInfoModuleState {

  @JsonProperty("name")
  private String name;

  @JsonProperty("active")
  private boolean active;

  @JsonProperty("more")
  private boolean more;

  @JsonProperty("done")
  private boolean done;

  @JsonProperty("mtu")
  private int mtu;

  @JsonProperty("guid")
  Guid guid;

  @JsonProperty("extra")
  AnyType extra;

  @JsonIgnore
  public String getName() {
    return name;
  }

  @JsonIgnore
  public boolean isActive() {
    return active;
  }

  @JsonIgnore
  public boolean isDone() {
    return done;
  }


  @JsonIgnore
  public boolean isMore() {
    return more;
  }

  @JsonIgnore
  public int getMtu() {
    return mtu;
  }

  @JsonIgnore
  public Guid getGuid() {
    return guid;
  }

  @JsonIgnore
  public AnyType getExtra() {
    return extra;
  }

  @JsonIgnore
  public void setName(String name) {
    this.name = name;
  }

  @JsonIgnore
  public void setActive(boolean active) {
    this.active = active;
  }

  @JsonIgnore
  public void setMore(boolean more) {
    this.more = more;
  }

  @JsonIgnore
  public void setDone(boolean done) {
    this.done = done;
  }

  @JsonIgnore
  public void setMtu(int mtu) {
    this.mtu = mtu;
  }

  @JsonIgnore
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  @JsonIgnore
  public void setExtra(AnyType extra) {
    this.extra = extra;
  }

}
