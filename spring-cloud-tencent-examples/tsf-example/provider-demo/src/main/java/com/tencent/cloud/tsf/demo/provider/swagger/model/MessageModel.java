/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.tsf.demo.provider.swagger.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "消息", title = "messageModel")
public class MessageModel {

	@Schema(name = "id", title = "消息ID", required = true, description = "消息ID notes")
	private String msgId;

	@Schema(title = "消息内容", required = false)
	private String msgContent;

	@Schema(title = "消息发送者", required = true)
	private MessageUser sendUser;

	@Schema(title = "消息接收者", required = true)
	private List<MessageUser> receiveUsers;

	@Schema(title = "消息发送时间", required = true)
	private long sendTime;

	@Schema(title = "消息投递箱", required = false)
	private MessageBox messageBox;

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}

	public MessageUser getSendUser() {
		return sendUser;
	}

	public void setSendUser(MessageUser sendUser) {
		this.sendUser = sendUser;
	}

	public List<MessageUser> getReceiveUsers() {
		return receiveUsers;
	}

	public void setReceiveUsers(List<MessageUser> receiveUsers) {
		this.receiveUsers = receiveUsers;
	}

	public long getSendTime() {
		return sendTime;
	}

	public void setSendTime(long sendTime) {
		this.sendTime = sendTime;
	}

	public MessageBox getMessageBox() {
		return messageBox;
	}

	public void setMessageBox(MessageBox messageBox) {
		this.messageBox = messageBox;
	}
}
