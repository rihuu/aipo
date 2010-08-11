/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2008 Aimluck,Inc.
 * http://aipostyle.com/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.note;

import java.util.List;
import java.util.jar.Attributes;

import org.apache.cayenne.query.SelectQuery;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTNote;
import com.aimluck.eip.cayenne.om.portlet.EipTNoteMap;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.note.util.NoteUtils;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALDataContext;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 伝言メモ依頼者検索データを管理するためのクラスです。 <br />
 */
public class NoteClientSelectData extends ALAbstractSelectData<EipTNoteMap>
    implements ALData {
  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
      .getLogger(NoteClientSelectData.class.getName());

  /** ポートレットにアクセスしているユーザ ID */
  private String userId;

  /** 新着数 */
  private int newNoteAllSum = 0;

  /** 受信未読数 */
  private int unreadReceivedNotesAllSum = 0;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   * @see com.aimluck.eip.common.ALAbstractSelectData#init(com.aimluck.eip.modules.actions.common.ALAction,
   *      org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
   */
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR,
          ALEipUtils.getPortlet(rundata, context).getPortletConfig()
              .getInitParameter("p2a-sort"));
      logger.debug("Init Parameter (Note) : "
          + ALEipUtils.getPortlet(rundata, context).getPortletConfig()
              .getInitParameter("p2a-sort"));
    }

    super.init(action, rundata, context);
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#selectList(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  protected List<EipTNoteMap> selectList(RunData rundata, Context context) {

    try {
      userId = Integer.toString(ALEipUtils.getUserId(rundata));
      newNoteAllSum = NoteUtils.getNewReceivedNoteAllSum(rundata, userId);
      unreadReceivedNotesAllSum = NoteUtils.getUnreadReceivedNotesAllSum(
          rundata, userId);

      SelectQuery query = getSelectQuery(rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSort(query, rundata, context);

      List<EipTNoteMap> list = ALDataContext.performQuery(EipTNoteMap.class,
          query);

      return buildPaginatedList(list);
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#selectDetail(org.apache.turbine.util.RunData,
   *      org.apache.velocity.context.Context)
   */
  protected EipTNoteMap selectDetail(RunData rundata, Context context) {
    return null;
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#getResultData(java.lang.Object)
   */
  protected Object getResultData(EipTNoteMap map) {
    try {
      EipTNote record = map.getEipTNote();

      NoteClientResultData rd = new NoteClientResultData();
      rd.initField();
      rd.setNoteId(record.getNoteId().intValue());
      rd.setClientName(ALCommonUtils.compressString(record.getClientName(),
          getStrLength()));
      rd.setNoteStat(map.getNoteStat());
      rd.setAcceptDate(record.getAcceptDate());

      String subject = "";
      if (record.getSubjectType().equals("0")) {
        subject = ALCommonUtils.compressString(record.getCustomSubject(),
            getStrLength());
      } else if (record.getSubjectType().equals("1")) {
        subject = "再度電話します";
      } else if (record.getSubjectType().equals("2")) {
        subject = "電話をしてください";
      } else if (record.getSubjectType().equals("3")) {
        subject = "電話がありました";
      } else if (record.getSubjectType().equals("4")) {
        subject = "伝言があります";
      }

      rd.setSubject(subject);

      if (NoteUtils.NOTE_STAT_NEW.equals(map.getNoteStat())) {
        rd.setNoteStat(NoteUtils.NOTE_STAT_NEW);
        rd.setNoteStatImage("images/note/note_new_message.gif");
        rd.setNoteStatImageDescription("新着");
      } else if (NoteUtils.NOTE_STAT_UNREAD.equals(map.getNoteStat())) {
        rd.setNoteStat(NoteUtils.NOTE_STAT_UNREAD);
        rd.setNoteStatImage("images/note/note_unread_message.gif");
        rd.setNoteStatImageDescription("未読");
      } else {
        rd.setNoteStat(NoteUtils.NOTE_STAT_READ);
        rd.setNoteStatImage("images/note/note_read_message.gif");
        rd.setNoteStatImageDescription("既読");
      }

      return rd;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#getResultDataDetail(java.lang.Object)
   */
  protected Object getResultDataDetail(EipTNoteMap obj) {
    return null;
  }

  /**
   * @see com.aimluck.eip.common.ALAbstractSelectData#getColumnMap()
   */
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("client_name", EipTNoteMap.EIP_TNOTE_PROPERTY + "."
        + EipTNote.CLIENT_NAME_PROPERTY);
    map.putValue("subject_type", EipTNoteMap.EIP_TNOTE_PROPERTY + "."
        + EipTNote.SUBJECT_TYPE_PROPERTY);
    map.putValue("accept_date", EipTNoteMap.EIP_TNOTE_PROPERTY + "."
        + EipTNote.ACCEPT_DATE_PROPERTY);
    map.putValue("note_stat", EipTNoteMap.NOTE_STAT_PROPERTY);
    return map;
  }

  // /**
  // * ソート用の <code>Criteria</code> を構築する．
  // *
  // * @param crt
  // * @return
  // */
  // protected Criteria buildCriteriaForListViewSort(Criteria crt,
  // RunData rundata, Context context) {
  // Criteria criteria = super.buildCriteriaForListViewSort(crt, rundata,
  // context);
  //
  // // [第二ソート]
  // String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
  // if (sort == null)
  // return criteria;
  // Attributes map = getColumnMap();
  // String crt_key = map.getValue(sort);
  // if (!EipTNoteConstants.ACCEPT_DATE.equals(crt_key)) {
  // // 受付時間でソートする．
  // criteria.addDescendingOrderByColumn(EipTNoteConstants.ACCEPT_DATE);
  // }
  // return criteria;
  // }

  /**
   * 検索条件を設定した Criteria を返す． <BR>
   *
   * @param rundata
   * @return
   */
  private SelectQuery getSelectQuery(RunData rundata, Context context) {
    return NoteUtils.getSelectQueryNoteList(rundata, context);
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName(String userId) {
    return NoteUtils.getUserName(userId);
  }

  public String getUserFullName(String userId) {
    try {
      ALEipUser user = ALEipUtils.getALEipUser(Integer.valueOf(userId)
          .intValue());
      return user.getAliasName().getValue();
    } catch (Exception e) {
      return "";
    }
  }

  public String getUserId(String userName) {
    return NoteUtils.getUserId(userName);
  }

  public int getNewNoteAllSum() {
    return newNoteAllSum;
  }

  public int getUnreadReceivedNotesAllSum() {
    return unreadReceivedNotesAllSum;
  }

}
