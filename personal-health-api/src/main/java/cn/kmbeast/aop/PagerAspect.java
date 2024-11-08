package cn.kmbeast.aop;

import cn.kmbeast.pojo.dto.query.base.QueryDto;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
//表示这个类是一个切面
@Aspect
@Component
public class PagerAspect {

    /**
     * 环绕通知，用于处理带有@Pager注解的方法
     *
     * @param joinPoint 连接点
     * @param pager     注解实例
     * @return 原方法执行的结果
     * @throws Throwable 异常
     */
//    环绕通知，表示当带有paper注解的方法调用时，会先进入handlePageableParams方法
    @Around("@annotation(pager)")
    public Object handlePageableParams(ProceedingJoinPoint joinPoint, Pager pager) throws Throwable {
//       获取query方法所有参数，放到object列表中
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
//            检查每个参数是否是QueryDto类型，检查编译类型
            if (arg instanceof QueryDto) {
                QueryDto queryDTO = (QueryDto) arg;
//                跳转到configPager对分页参数进行修改
                configPager(queryDTO);
            }
        }
//        ，用新得到的queryDTO继续执行query方法
        return joinPoint.proceed(args);
    }

    /**
     * 分页参数转换逻辑
     *
     * @param queryDTO 分页参数DTO
     */
    private void configPager(QueryDto queryDTO) {
        if (queryDTO.getCurrent() != null && queryDTO.getSize() != null) {
            queryDTO.setCurrent((queryDTO.getCurrent() - 1) * queryDTO.getSize());
        }
    }
}
