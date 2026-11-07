import { Controller, Get } from "@nestjs/common";

@Controller()
export class HealthController {
  @Get("actuator/health")
  health(): { status: string } {
    return { status: "UP" };
  }
}
