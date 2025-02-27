/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.cloud.tsf.demo.provider.swagger.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.tsf.demo.provider.swagger.model.MessageBox;
import com.tencent.cloud.tsf.demo.provider.swagger.model.MessageModel;
import com.tencent.cloud.tsf.demo.provider.swagger.model.MessageUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("/swagger")
@Tag(description = "swagger 测试", name = "swaggerValue1")
public class SwaggerApiController {

	@RequestMapping(value = "/swagger/findMessages", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@Operation(method = "POST",
			summary = "根据任务ID查询任务列表",
			description = "根据任务ID查询任务列表Note")
	public List<MessageModel> findMessages(@RequestBody
	@Parameter(name = "msgIds", description = "消息ID列表") List<String> msgIds) {
		List<MessageModel> messageModels = new ArrayList<>();
		MessageModel messageModel = new MessageModel();
		messageModel.setMsgContent("test1");
		messageModel.setMsgId("1");
		messageModel.setSendTime(System.currentTimeMillis());
		MessageUser messageSender = new MessageUser();
		messageSender.setEmail("abc@xxxx.com");
		messageSender.setName("特朗普");
		messageSender.setOfficeAddress("华盛顿白宫");
		messageSender.setPhoneNum("911911911");
		messageModel.setSendUser(messageSender);
		MessageUser messageReceiver = new MessageUser();
		messageReceiver.setEmail("abc@xxxx.com");
		messageReceiver.setName("拜登");
		messageReceiver.setOfficeAddress("华盛顿白宫");
		messageReceiver.setPhoneNum("911911911");
		messageModel.setReceiveUsers(Collections.singletonList(messageReceiver));
		messageModels.add(messageModel);
		return messageModels;
	}

	//虽然这些@ExampleProperty和@Example属性已经在Swagger中实现，但Springfox还没有支持它们。问题仍然存在：
	//https://github.com/springfox/springfox/issues/853
	//https://github.com/springfox/springfox/issues/1536

	@Operation(summary = "获取消息内容", method = "GET", description = "获取消息内容Note")
	@ApiResponse(responseCode = "200", description = "abcdef", content = @Content(schema = @Schema(implementation = String.class)))
	@RequestMapping("/swagger/getMessageContent")
	public String getMessageContent(@RequestParam(name = "id")
	@Parameter(description = "消息ID", name = "id") String msgId) {
		return "abcdefg";
	}

	@Operation(summary = "获取消息详情", method = "GET", description = "获取消息内容Note")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "abcdef", content = @Content(schema = @Schema(implementation = MessageModel.class)))})
	@RequestMapping(value = "/swagger/getMessageDetail", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public MessageModel getMessageDetail(@RequestParam(name = "id")
	@Parameter(description = "消息ID", name = "id") String msgId) {
		MessageModel messageModel = new MessageModel();
		messageModel.setMsgContent("test1");
		messageModel.setMsgId("1");
		messageModel.setSendTime(System.currentTimeMillis());
		MessageUser messageSender = new MessageUser();
		messageSender.setEmail("abc@xxxx.com");
		messageSender.setName("特朗普");
		messageSender.setOfficeAddress("华盛顿白宫");
		messageSender.setPhoneNum("911911911");
		messageModel.setSendUser(messageSender);
		MessageUser messageReceiver = new MessageUser();
		messageReceiver.setEmail("abc@xxxx.com");
		messageReceiver.setName("拜登");
		messageReceiver.setOfficeAddress("华盛顿白宫");
		messageReceiver.setPhoneNum("911911911");
		messageModel.setReceiveUsers(Collections.singletonList(messageReceiver));
		return messageModel;
	}

	@Operation(summary = "获取投递箱详情", method = "GET", description = "")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "获取投递箱成功", content = @Content(schema = @Schema(implementation = MessageBox.class)))})
	@RequestMapping(value = "/swagger/getMessageBox", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public MessageBox getMessageBox(@RequestParam @Parameter(required = true, description = "投递箱ID") String boxId,
			@RequestParam @Parameter(name = "sizeLimit", example = "10", description = "投递箱最大投递数") int maxSizeLimit) {
		return new MessageBox();
	}

	@Operation(summary = "获取投递箱详情V2", method = "POST", description = "")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "获取投递箱成功", content = @Content(schema = @Schema(implementation = MessageBox.class)))})
	@RequestMapping(value = "/swagger/v2/getMessageBox", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public MessageBox getMessageBoxV2(@RequestBody
	@Parameter(required = true, name = "messageBox", example = "投递箱信息") MessageBox messageBox) {
		return new MessageBox();
	}

	@Operation(summary = "获取投递箱详情V3", method = "POST", description = "")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "获取投递箱成功", content = @Content(schema = @Schema(implementation = Map.class)))})
	@RequestMapping(value = "/swagger/v3/getMessageBox", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Map<String, Object> queryMessageBoxV3(@RequestBody
	@Parameter(required = true, name = "messageBox", example = "投递箱信息") MessageBox messageBox) {

		return new HashMap<>();
	}


	@Operation(summary = "获取投递箱地址", method = "GET", description = "")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "投递箱地址", content = @Content(schema = @Schema(implementation = String.class)))})
	@RequestMapping(value = "/swagger/getMessageBoxAddress", produces = MediaType.TEXT_PLAIN_VALUE)
	public String queryMessageBoxAddress(@RequestParam(name = "boxId")
	@Parameter(description = "投递箱ID", name = "boxId", example = "box-qvp9htm5", required = true) String id) {
		return "华盛顿白宫";

	}

}
