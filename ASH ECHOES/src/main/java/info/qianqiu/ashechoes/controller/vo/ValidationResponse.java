package info.qianqiu.ashechoes.controller.vo;

import lombok.Data;

@Data
public class ValidationResponse {
    private String plainToken;
    private String signature;
}