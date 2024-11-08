package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.HealthModelConfigMapper;
import cn.kmbeast.mapper.UserHealthMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.base.QueryDto;
import cn.kmbeast.pojo.dto.query.extend.HealthModelConfigQueryDto;
import cn.kmbeast.pojo.dto.query.extend.UserHealthQueryDto;
import cn.kmbeast.pojo.em.IsReadEnum;
import cn.kmbeast.pojo.em.MessageType;
import cn.kmbeast.pojo.entity.HealthModelConfig;
import cn.kmbeast.pojo.entity.Message;
import cn.kmbeast.pojo.entity.UserHealth;
import cn.kmbeast.pojo.vo.ChartVO;
import cn.kmbeast.pojo.vo.MessageVO;
import cn.kmbeast.pojo.vo.UserHealthVO;
import cn.kmbeast.service.MessageService;
import cn.kmbeast.service.UserHealthService;
import cn.kmbeast.utils.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户健康记录业务逻辑实现
 */
@Service
public class UserHealthServiceImpl implements UserHealthService {

    @Resource
    private UserHealthMapper userHealthMapper;
    @Resource
    private HealthModelConfigMapper healthModelConfigMapper;
    @Resource
    private MessageService messageService;

    /**
     * 用户健康记录新增
     *
     * @param userHealths 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> save(List<UserHealth> userHealths) {
        dealMessage(userHealths);
        dealRole(userHealths);
        userHealthMapper.batchSave(userHealths);
        return ApiResult.success();
    }

    public void dealRole(List<UserHealth> userHealths) {
        LocalDateTime nowTime = LocalDateTime.now();
        userHealths.forEach(userHealth -> {
            // 获取当前用户的ID
            Integer userId = LocalThreadHolder.getUserId();
            userHealth.setUserId(userId);
            userHealth.setCreateTime(nowTime);
        });
    }
    /**
     * 如果有异常指标情况，此方法做通知转发
     *
     * @param userHealths 用户健康记录集合
     */
    private void dealMessage(List<UserHealth> userHealths) {
//        创建存储消息的列表，对应每个userHealths列表
        List<Message> messageList = new ArrayList<>();
//        遍历userHealths列表
        userHealths.forEach(userHealth -> {
            Integer healthModelConfigId = userHealth.getHealthModelConfigId();//从用户健康表得到健康模型id
            HealthModelConfigQueryDto queryDto = new HealthModelConfigQueryDto();//定义健康模型查询类
            queryDto.setId(healthModelConfigId);//设置健康模型查询类的健康模型id
            List<HealthModelConfig> healthModelConfigs = healthModelConfigMapper.query(queryDto);//通过健康模型id得到对应的健康模型列表
            if (!CollectionUtils.isEmpty(healthModelConfigs)) {
                HealthModelConfig healthModelConfig = healthModelConfigs.get(0);//得到第一个健康模型
                // 值范围为：101,230
                String valueRange = healthModelConfig.getValueRange();//获取该模型正常值范围
                String[] values = valueRange.split(",");
                // 最小值
                int mixValue = Integer.parseInt(values[0]);
                // 最大值
                int maxValue = Integer.parseInt(values[1]);
                // 如果用户输入的指标是超出正常范围的，需要通知用户处理
                int value = Integer.parseInt(userHealth.getValue());//得到用户健康的上传的值
                // 异常情况
                if (value < mixValue || value > maxValue) {
                    // 封装消息体
                    Message message = sendMessage(healthModelConfig, userHealth);//将对应的健康模型和用户健康表及进行处理
                    messageList.add(message);
                }
            }
        });
        if(!CollectionUtils.isEmpty(messageList)){
            // 丢给消息业务逻辑处理，将message列表里的信息进行保存
            messageService.dataWordSave(messageList);
        }
    }

    /**
     * 处理符合消息通知的用户健康记录
     *
     * @param userHealth 用户健康记录
     * @return List<Message>
     */
    private Message sendMessage(HealthModelConfig healthModelConfig, UserHealth userHealth) {
        Message message = new Message();
        // 消息体内容
        message.setContent("你记录的【" + healthModelConfig.getName() + "】超标了，正常值范围:[" + healthModelConfig.getValueRange() + "]，请注意休息。必要时请就医!");
        // 指标提醒类通知，设置message的type
        message.setMessageType(MessageType.DATA_MESSAGE.getType());
        // 接收者
        message.setReceiverId(LocalThreadHolder.getUserId());
        // 是否已读
        message.setIsRead(IsReadEnum.READ_NO.getStatus());
        // 设置消息提醒时间
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    /**
     * 用户健康记录删除
     *
     * @param ids 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> batchDelete(List<Long> ids) {
        userHealthMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 用户健康记录修改
     *
     * @param userHealth 参数
     * @return Result<Void>
     */
    @Override
    public Result<Void> update(UserHealth userHealth) {
        userHealthMapper.update(userHealth);
        return ApiResult.success();
    }

    /**
     * 用户健康记录查询
     *
     * @param userHealthQueryDto 查询参数
     * @return Result<List < UserHealthVO>>
     */
    @Override
    public Result<List<UserHealthVO>> query(UserHealthQueryDto userHealthQueryDto) {
        List<UserHealthVO> userHealthVOS = userHealthMapper.query(userHealthQueryDto);
        Integer totalCount = userHealthMapper.queryCount(userHealthQueryDto);
        return PageResult.success(userHealthVOS, totalCount);
    }

    /**
     * 统计模型存量数据
     *
     * @return Result<List < ChartVO>> 响应结果
     */
    @Override
    public Result<List<ChartVO>> daysQuery(Integer day) {
        QueryDto queryDto = DateUtil.startAndEndTime(day);
        UserHealthQueryDto userHealthQueryDto = new UserHealthQueryDto();
        userHealthQueryDto.setStartTime(queryDto.getStartTime());
        userHealthQueryDto.setEndTime(queryDto.getEndTime());
        List<UserHealthVO> userHealthVOS = userHealthMapper.query(userHealthQueryDto);
        List<LocalDateTime> localDateTimes = userHealthVOS.stream().map(UserHealthVO::getCreateTime).collect(Collectors.toList());
        List<ChartVO> chartVOS = DateUtil.countDatesWithinRange(day, localDateTimes);
        return ApiResult.success(chartVOS);
    }


}