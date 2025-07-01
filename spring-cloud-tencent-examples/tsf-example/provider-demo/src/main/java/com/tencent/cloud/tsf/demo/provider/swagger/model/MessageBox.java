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


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "消息投递箱")
public class MessageBox {

	@Schema(title = "默认失效天数", required = false)
	private int expiredDays;

	@Schema(title = "最大失效天数", required = false)
	private Integer maxExpiredDays;

	@Schema(title = "容量大小", required = false)
	private Float capacity;

	@Schema(title = "最大容量大小", required = false)
	private float maxCapacity;

	@Schema(title = "接受的信息数量", required = false)
	private Double size;

	@Schema(title = "最大接受的信息数量", required = false)
	private double maxSize;

	@Schema(title = "消息(循环测试嵌套对象)", required = false)
	private MessageModel messageModel;

	public int getExpiredDays() {
		return expiredDays;
	}

	public void setExpiredDays(int expiredDays) {
		this.expiredDays = expiredDays;
	}

	public Integer getMaxExpiredDays() {
		return maxExpiredDays;
	}

	public void setMaxExpiredDays(Integer maxExpiredDays) {
		this.maxExpiredDays = maxExpiredDays;
	}

	public Float getCapacity() {
		return capacity;
	}

	public void setCapacity(Float capacity) {
		this.capacity = capacity;
	}

	public float getMaxCapacity() {
		return maxCapacity;
	}

	public void setMaxCapacity(float maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public Double getSize() {
		return size;
	}

	public void setSize(Double size) {
		this.size = size;
	}

	public double getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(double maxSize) {
		this.maxSize = maxSize;
	}

	public MessageModel getMessageModel() {
		return messageModel;
	}

	public void setMessageModel(MessageModel messageModel) {
		this.messageModel = messageModel;
	}
}
