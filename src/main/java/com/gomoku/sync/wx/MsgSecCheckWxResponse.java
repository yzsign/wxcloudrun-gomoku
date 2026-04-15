package com.gomoku.sync.wx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code wxa/msg_sec_check} 2.0 返回（仅解析校验所需字段）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MsgSecCheckWxResponse {

    private Integer errcode;
    private String errmsg;
    private Result result;

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        /** pass / review / risky 等，以微信文档为准 */
        private String suggest;

        public String getSuggest() {
            return suggest;
        }

        public void setSuggest(String suggest) {
            this.suggest = suggest;
        }
    }
}
