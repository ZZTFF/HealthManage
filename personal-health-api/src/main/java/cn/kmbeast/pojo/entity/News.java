package cn.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 健康资讯实体
 */
@Data
public class News {
    /**
     * 主键ID
     */
    private Integer id;
    /**
     * 资讯标题
     */
    private String name;
    /**
     * 资讯的内容
     */
    private String content;
    /**
     * 标签的ID
     */
    private Integer tagId;
    /**
     * 封面
     */
    private String cover;
    /**
     * 阅读者的ID列表，以“,”进行分割
     */
    private String readerIds;
    /**
     * 是否推荐
     */
    private Boolean isTop;
    /**
     * 发布时间
     */
//    当序列化一个包含 createTime 属性的Java对象时，应该将该属性（一个 LocalDateTime 类型）
//    格式化为一个遵循 “yyyy-MM-dd HH:mm:ss” 格式的字符串。
//    同样地，当反序列化JSON字符串时，也应该按照这个格式来解析日期时间字符串到 LocalDateTime 对象。
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
