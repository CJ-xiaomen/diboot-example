package com.example.file.handler;

import com.diboot.core.exception.BusinessException;
import com.diboot.core.handler.DefaultExceptionHandler;
import com.diboot.core.util.S;
import com.diboot.core.util.V;
import com.diboot.core.vo.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 启用devtools，该文件将由diboot-devtools自动生成
 */
/**
 * 通用异常处理类
 * @author www.dibo.ltd
 * @version v2.0
 * @date 2019/7/19
 */
@ControllerAdvice
@Slf4j
public class GeneralExceptionHandler {

    /**
     * 统一处理校验错误 BindResult
     * @param ex
     * @return
     */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public Object validExceptionHandler(Exception ex){
        Map<String, Object> map = new HashMap<>();
        BindingResult br = null;
        if(ex instanceof BindException){
            br = ((BindException)ex).getBindingResult();
        }
        else if(ex instanceof MethodArgumentNotValidException){
            br = ((MethodArgumentNotValidException)ex).getBindingResult();
        }
        if (br != null && br.hasErrors()) {
            map.put("code", Status.FAIL_VALIDATION.code());
            String validateErrorMsg = V.getBindingError(br);
            map.put("msg", validateErrorMsg);
            log.warn("数据校验失败, {}: {}", br.getObjectName(), validateErrorMsg);
        }
        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    /**
     * 统一异常处理类
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(HttpServletRequest request, Exception e) {
        HttpStatus status = getStatus(request);
        Map<String, Object> map = null;
        if(e instanceof BusinessException){
            BusinessException be = (BusinessException)e;
            map = be.toMap();
        }
        else{
            map = new HashMap<>();
            map.put("code", status.value());
            String msg = e.getMessage();
            //空指针异常
            if(msg == null){
                msg = e.getClass().getSimpleName();
            }
            map.put("msg", msg);
        }
        log.warn("请求处理异常", e);
        if(isJsonRequest(request)) {
            return new ResponseEntity<>(map, HttpStatus.OK);
        }
        else {
            //获取错误页面
            String viewName = getViewName(request, e);
            map.put("exception", e);
            map.put("status", status.value());
            map.put("message", map.get("msg"));
            map.put("timestamp", new Date());
            map.remove("msg");
            return new ModelAndView(viewName, map);
        }
    }

    /**
     * 获取默认的错误页面
     * @param request
     * @param ex
     * @return
     */
    protected String getViewName(HttpServletRequest request, Exception ex){
        return "error";
    }

    /**
     * 是否为JSON数据请求
     * @param request
     * @return
     */
    protected boolean isJsonRequest(HttpServletRequest request){
        if("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))){
            return true;
        }
        return S.containsIgnoreCase(request.getHeader("Accept"),"json")
                || S.containsIgnoreCase(request.getHeader("content-type"), "json")
                || S.containsIgnoreCase(request.getContentType(), "json");
    }

    /**
     * 获取状态码
     * @param request
     * @return
     */
    protected HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        }
        catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

}