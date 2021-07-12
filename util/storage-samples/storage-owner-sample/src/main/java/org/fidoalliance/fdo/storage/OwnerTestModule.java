package org.fidoalliance.fdo.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.serviceinfo.Module;

/**
 * Test unknown module.
 */
public class OwnerTestModule implements Module {

  private static final String TEST_MOD_NAME = "test_mod";
  private static final String TEST_MOD_ACTIVE = TEST_MOD_NAME + ":active";
  private final DataSource ds;
  private Composite state;

  /**
   * Constructs a OwnerTestModule.
   *
   * @param ds A SQL Datasource.
   */
  public OwnerTestModule(DataSource ds) {
    this.ds = ds;
    state = Composite.newArray();
    state.set(Const.FIRST_KEY, false);
    state.set(Const.SECOND_KEY, Composite.newArray());
    state.set(Const.THIRD_KEY, false);
  }


  @Override
  public String getName() {
    return TEST_MOD_NAME;
  }

  @Override
  public void prepare(UUID guid) {

    state.set(Const.FIRST_KEY, true);
    state.set(Const.THIRD_KEY, true);
    String sql = "SELECT CONTENT "
        + "FROM SYSTEM_MODULE_RESOURCE "
        + "WHERE CONTENT_TYPE_TAG = 'test_mod:active' "
        + "ORDER BY PRIORITY ASC";
    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          byte[] content = rs.getBytes(1);
          if (content.length == 1 && content[0] == 0xF5) {
            state.set(Const.SECOND_KEY,
                Composite.newArray().set(Const.FIRST_KEY, TEST_MOD_ACTIVE)
                    .set(Const.SECOND_KEY, true));
          } else {
            state.set(Const.SECOND_KEY,
                Composite.newArray().set(Const.FIRST_KEY, TEST_MOD_ACTIVE)
                    .set(Const.SECOND_KEY, false));
          }
          state.set(Const.THIRD_KEY, false);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setMtu(int mtu) {

  }

  @Override
  public void setState(Composite state) {

    this.state = state;
  }

  @Override
  public Composite getState() {
    return state;
  }

  @Override
  public void setServiceInfo(Composite kvPair, boolean isMore) {

    System.out.println(kvPair.getAsString(Const.FIRST_KEY));
    state.set(Const.THIRD_KEY, true);

  }

  @Override
  public boolean isMore() {
    return false;
  }

  @Override
  public boolean isDone() {
    return state.getAsBoolean(Const.THIRD_KEY);
  }

  @Override
  public boolean hasMore() {
    if (state.size() > 0) {
      if (state.getAsComposite(Const.SECOND_KEY).size() > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Composite nextMessage() {
    Composite result = state.getAsComposite(Const.SECOND_KEY);
    state.set(Const.SECOND_KEY, Composite.newArray());
    return result;
  }
}
