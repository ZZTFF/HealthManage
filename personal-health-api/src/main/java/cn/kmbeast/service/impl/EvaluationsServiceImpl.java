package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.EvaluationsMapper;
import cn.kmbeast.mapper.MessageMapper;
import cn.kmbeast.mapper.UserMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.EvaluationsQueryDto;
import cn.kmbeast.pojo.em.IsReadEnum;
import cn.kmbeast.pojo.em.MessageType;
import cn.kmbeast.pojo.entity.Evaluations;
import cn.kmbeast.pojo.entity.Message;
import cn.kmbeast.pojo.entity.User;
import cn.kmbeast.pojo.vo.CommentChildVO;
import cn.kmbeast.pojo.vo.CommentParentVO;
import cn.kmbeast.pojo.vo.EvaluationsVO;
import cn.kmbeast.service.EvaluationsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 评论服务实现类
 */
@Service
public class EvaluationsServiceImpl implements EvaluationsService {

    @Resource
    private EvaluationsMapper evaluationsMapper;
    @Resource
    private UserMapper userMapper;

    @Resource
    private MessageMapper messageMapper;

    /**
     * 评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> insert(Evaluations evaluations) {
//在前端返回给后端时，已经有了的字段，parentId、contentType、contentId、content
//还差的字段：commentterId、replierId、upvoteList、createTime
        evaluations.setCommenterId(LocalThreadHolder.getUserId());//获取当前用户id为评论者id
        //创建queryConditionEntity，初始化他的id为评论者id
        User queryConditionEntity = User.builder().id(LocalThreadHolder.getUserId()).build();
        User user = userMapper.getByActive(queryConditionEntity);//获取这个user相关信息
        if (user.getIsWord()) {
            return ApiResult.error("账户已被禁言");
        }
        // TODO 需要发通知！保存到message表发通知
        evaluations.setCreateTime(LocalDateTime.now());
        evaluationsMapper.save(evaluations);
        //对应处理message表
        List<Message> messageList = new ArrayList<>();
        Message message = new Message();
//        设置content
        message.setContent("你的评论被回复了");
//        设置message_type
        message.setMessageType(MessageType.EVALUATIONS_BY_REPLY.getType());
//        设置idRead
        message.setIsRead(IsReadEnum.READ_NO.getStatus());
//        还需要设置receiverId、senderId设置为null即可
//        通过保存evaluations时设置的parent_id得到以parent_id为id的评论表行，从中得到commenter_id，这个commenter_id就是要的receiverId
        if(evaluationsMapper.queryId(evaluations.getParentId())!= 0){
            Integer receiverId = evaluationsMapper.queryId(evaluations.getParentId());//此时可能得到的是一个列表
            message.setReceiverId(receiverId);
            message.setContentId(evaluations.getContentId());
            message.setCreateTime(LocalDateTime.now());
            //将当前用户的id设置为被回复评论的评论表中的replier_id,即对评论表做跟更新操作
            evaluationsMapper.updateReplierId(evaluations.getParentId(), LocalThreadHolder.getUserId());
            messageList.add(message);
            messageMapper.batchSave(messageList);
        }

        return ApiResult.success("评论成功");
    }

    /**
     * 查询全部评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> list(Integer contentId, String contentType) {
        List<CommentParentVO> parentComments = evaluationsMapper.getParentComments(contentId, contentType);
        setUpvoteFlag(parentComments);
        Integer count = evaluationsMapper.totalCount(contentId, contentType);
        return ApiResult.success(new EvaluationsVO(count, parentComments));
    }

    /**
     * 设置点赞状态
     *
     * @param parentComments 评论数据列表
     */
    private void setUpvoteFlag(List<CommentParentVO> parentComments) {
        String userId = LocalThreadHolder.getUserId().toString(); // 预先获取用户ID
        parentComments.forEach(parentComment -> {
            parentComment.setUpvoteFlag(isUserUpvote(parentComment.getUpvoteList(), userId));
            parentComment.setUpvoteCount(countVotes(parentComment.getUpvoteList()));
            Optional.ofNullable(parentComment.getCommentChildVOS())
                    .orElse(Collections.emptyList())
                    .forEach(child -> {
                        child.setUpvoteFlag(isUserUpvote(child.getUpvoteList(), userId));
                        child.setUpvoteCount(countVotes(child.getUpvoteList()));
                    });
        });
    }

    /**
     * 判断用户是否已点赞
     *
     * @param voteStr 点赞用户ID字符串（逗号分隔）
     * @param userId  用户ID
     * @return 是否已点赞
     */
    private boolean isUserUpvote(String voteStr, String userId) {
        return Optional.ofNullable(voteStr)
                .map(s -> Arrays.asList(s.split(",")))
                .orElse(Collections.emptyList())
                .contains(userId);
    }

    /**
     * 计算点赞数
     *
     * @param voteStr 点赞用户ID字符串（逗号分隔）
     * @return 点赞数
     */
    private int countVotes(String voteStr) {
        return Optional.ofNullable(voteStr)
                .map(s -> s.split(",").length)
                .orElse(0);
    }

    /**
     * 分页查询评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> query(EvaluationsQueryDto evaluationsQueryDto) {
        List<CommentChildVO> list = evaluationsMapper.query(evaluationsQueryDto);
        Integer totalPage = evaluationsMapper.queryCount(evaluationsQueryDto);
        return PageResult.success(list, totalPage);
    }

    /**
     * 批量删除评论数据
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> batchDelete(List<Integer> ids) {
        evaluationsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 评论删除
     *
     * @return Result<String>
     */
    @Override
    public Result<String> delete(Integer id) {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(id);
        evaluationsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 评论修改
     *
     * @return Result<String>
     */
    @Override
    public Result<Void> update(Evaluations evaluations) {
        // TODO 点赞需要做通知
//        评论被别人点赞了，需要通知该评论的人
        evaluationsMapper.update(evaluations);
        List<Message> messageList = new ArrayList<>();
        Message message = new Message();
        message.setContent("你的评论被点赞了");
        message.setMessageType(MessageType.EVALUATIONS_BY_UPVOTE.getType());
//        设置当前用户id为message的ReceiverId
        message.setReceiverId(LocalThreadHolder.getUserId());
        message.setIsRead(IsReadEnum.READ_NO.getStatus());
        message.setContentId(evaluations.getContentId());
        message.setCreateTime(LocalDateTime.now());
        messageList.add(message);
        messageMapper.batchSave(messageList);
        return ApiResult.success();
    }
}
