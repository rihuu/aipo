/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.facilities;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipMFacilityGroup;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.facilities.util.FacilitiesUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;

/**
 * 設備の順番情報のフォームデータを管理するためのクラスです。 <br />
 */
public class FacilityGroupChangeTurnFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(FacilityGroupChangeTurnFormData.class.getName());

  // 設備グループ名のリスト　ソート後
  private ALStringField positions;

  // 施設グループIDs
  private String[] facilityGroupIds = null;

  /** 設備グループのリスト */
  private List<FacilityGroupResultData> facilityGroupList = null;

  private List<EipMFacilityGroup> rawFacilityGroupList = null;

  /**
   * 初期化します。
   * 
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    super.init(action, rundata, context);

    facilityGroupList = new ArrayList<FacilityGroupResultData>();
    rawFacilityGroupList = new ArrayList<EipMFacilityGroup>();
  }

  /**
   * 各フィールドを初期化します。 <BR>
   * 
   * 
   */
  @Override
  public void initField() {
    // ユーザ名のリスト
    positions = new ALStringField();
    positions.setFieldName("設備グループ名リスト");
    positions.setTrim(true);
  }

  /**
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res = true;
    try {
      res = super.setFormData(rundata, context, msgList);
      if (res) {
        // 初期
        if (positions.getValue() == null || positions.getValue().equals("")) {
          facilityGroupList = FacilitiesUtils.getFacilityGroupAllList();
        } else {// データ送信時
          StringTokenizer st = new StringTokenizer(positions.getValue(), ",");
          facilityGroupIds = new String[st.countTokens()];
          int count = 0;
          while (st.hasMoreTokens()) {
            facilityGroupIds[count] = st.nextToken();
            count++;
          }
          SelectQuery<EipMFacilityGroup> query =
            Database.query(EipMFacilityGroup.class);
          List<EipMFacilityGroup> list = query.fetchList();

          for (int i = 0; i < facilityGroupIds.length; i++) {
            EipMFacilityGroup facilityGroup =
              getEipMFacilityGroupFromFacilityId(list, facilityGroupIds[i]);
            facilityGroupList.add(FacilitiesUtils
              .getFacilityGroupResultData(facilityGroup));
            rawFacilityGroupList.add(facilityGroup);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("facilities", ex);
      return false;
    }
    return res;
  }

  /**
   * 各フィールドに対する制約条件を設定します。 <BR>
   * 
   * 
   */
  @Override
  protected void setValidator() {
  }

  /**
   * フォームに入力されたデータの妥当性検証を行います。 <BR>
   * 
   * @param msgList
   * @return
   */
  @Override
  protected boolean validate(List<String> msgList) {
    return (msgList.size() == 0);
  }

  /**
   * 『設備』を読み込みます。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      return true;
    } catch (Exception e) {
      logger.error("facilities", e);
      return false;
    }
  }

  /**
   * 『設備』を追加します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   * 『設備』を更新します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    boolean res = true;
    try {
      int newPosition = 1;
      for (EipMFacilityGroup facilityGroup : rawFacilityGroupList) {
        facilityGroup.setSort(newPosition);
        newPosition++;
      }
      Database.commit();
    } catch (Exception e) {
      Database.rollback();
      logger.error("facilities", e);
      res = false;
    }
    return res;
  }

  /**
   * 『設備』を削除します。 <BR>
   * 
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   * 指定したchar型文字が記号であるかを判断します。
   * 
   * @param ch
   * @return
   */
  protected boolean isSymbol(char ch) {
    byte[] chars;

    try {
      chars = (Character.valueOf(ch).toString()).getBytes("shift_jis");
    } catch (UnsupportedEncodingException ex) {
      return false;
    }

    if (chars == null
      || chars.length == 2
      || Character.isDigit(ch)
      || Character.isLetter(ch)) {
      return false;
    } else {
      return true;
    }

  }

  /**
   * 指定した設備グループIDのオブジェクトを取得する．
   * 
   * @param userList
   * @param userName
   * @return
   */
  private EipMFacilityGroup getEipMFacilityGroupFromFacilityId(
      List<EipMFacilityGroup> facilityGroupList, String facilityGroupId) {
    for (int i = 0; i < facilityGroupList.size(); i++) {
      EipMFacilityGroup facilityGroup = facilityGroupList.get(i);
      if (facilityGroup.getGroupId().toString().equals(facilityGroupId)) {
        return facilityGroup;
      }
    }
    return null;
  }

  /**
   * ユーザグループリストを取得する．
   * 
   * @return
   */
  public List<FacilityGroupResultData> getFacilityGroupList() {
    return facilityGroupList;
  }

}