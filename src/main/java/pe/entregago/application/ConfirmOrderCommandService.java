package pe.entregago.application;

import pe.entregago.domain.model.commands.ConfirmOrderCommand;

public interface ConfirmOrderCommandService { ConfirmOrderResult handle(ConfirmOrderCommand command); }
