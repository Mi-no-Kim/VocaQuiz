package com.vocaquiz.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channel_producer",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_channel_producer",
        columnNames = {"channel_id", "producer_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelProducer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    public static ChannelProducer create(Channel channel, Producer producer) {
        ChannelProducer channelProducer = new ChannelProducer();
        channelProducer.channel = channel;
        channelProducer.producer = producer;

        return channelProducer;
    }
}
