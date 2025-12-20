package dev.rhizome.rumor.chat;

public enum ChatStatus {
    /** 等待： 默认状态，可以被搜索状态的村民拉进对话 */
    WAITING,
    /** 搜索： 正在搜索对话参与者，不会被拉进其他对话 */
    SEARCHING,
    /** 聊天： 已经有聊天上下文，不会被拉进其他对话 */
    CHATTING,
    /** 冷却： 正在冷却，不会做除了解除冷却外的任何事 */
    COOLDOWN
}
