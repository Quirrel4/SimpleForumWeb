package com.he.community.utils;

public class CommunityConstant {
    public static final int ACTIVATION_SUCCESS=0;
    public static final int ACTIVATION_REPEAT=1;
    public static final int ACTIVATION_FAILURE=2;

    //默认登录凭证超时时间,12小时
    public static final int DEFAULT_EXPIRED_SECONDS=3600*12;
    //记住凭证状态超时时间,一百天
    public static final  int REMEM_EXPIRED_SECONDS=3600*12*100;
  /**
   * 实体类型：帖子
   */
    public static final int ENTITY_TYPE_POST=1;
  /**
   * 实体类型：评论
   */
    public static final  int ENTITY_TYPE_COMMENT=2;

  /**
   * 实体类型：用户
   */
    public static final int ENTITY_TYPE_USER=3;

  /**
   * 主题：评论
   */
  public static final String TOPIC_COMMENT="comment";

  /**
   * 主题：点赞
   */
  public static final String TOPIC_LIKE="like";

  /**
   * 主题：关注
   */
   public static final String TOPIC_FOLLOW="follow";


    /**
     * 主题：发帖
     */
   public static final String TOPIC_PUBLISH="publish";

    /**
     * 主题: 删帖
     */
    public static final String TOPIC_DELETE="delete";

    /**
     * 主题: 分享
     */
    public static final String TOPIC_SHARE="share";


    /**
     * 系统用户ID
     */
    public static final int SYSTEM_USER_ID=1;


    /**
     * 权限：普通用户
     */
    public static final String AUTHORITY_USER="user";

    /**
     * 权限：管理员
     */
    public static final String AUTHORITY_ADMIN="admin";

    /**
     * 权限：版主
     */
    public static final String AUTHORITY_MODERATOR="moderator";



}
