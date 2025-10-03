package com.study.estoqueservice.service;

import org.apache.kafka.common.protocol.types.Field;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EstoqueListener {
    @KafkaListener(topics = "estoque-topico", groupId = "estoque-group")
    public void processarVenda(String mensagem){
        //logica para atualizar o estoque
        System.out.println("Venda recebida no estoque: " + mensagem);
    }
}
