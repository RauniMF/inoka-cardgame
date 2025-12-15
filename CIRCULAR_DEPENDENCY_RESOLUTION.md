# Circular Dependency Resolution - Design Document

## Problem Statement

The application had a circular dependency between two Spring services:
- **GameService** depended on **SchedulerService** (to queue game updates for broadcasting)
- **SchedulerService** depended on **GameService** (to retrieve game data for broadcasting)

This circular dependency is problematic because:
1. It makes the code harder to understand and maintain
2. It can cause issues with dependency injection frameworks
3. It violates the Dependency Inversion Principle (one of the SOLID principles)
4. It makes testing more difficult
5. It can lead to runtime errors in some Spring configurations

## Industry Standard Solution: Event-Driven Architecture

The recommended approach for resolving circular dependencies is to break the coupling using **events**. This follows the **Observer Pattern** and implements a **publish-subscribe** mechanism.

### Why Event-Driven Architecture?

1. **Loose Coupling**: Services don't need direct references to each other
2. **Single Responsibility**: Each service focuses on its own concerns
3. **Extensibility**: New listeners can be added without modifying existing code
4. **Testability**: Services can be tested in isolation
5. **Spring Native Support**: Spring Framework has built-in event publishing capabilities

## Implementation

### 1. Created GameUpdateEvent

```java
public class GameUpdateEvent extends ApplicationEvent {
    private final Game game;
    
    public GameUpdateEvent(Object source, Game game) {
        super(source);
        this.game = game;
    }
    
    public Game getGame() {
        return game;
    }
    
    public String getGameId() {
        return game.getId();
    }
}
```

**Design Decision**: The event contains the full `Game` object rather than just the `gameId`. This eliminates the need for SchedulerService to query GameService, achieving complete decoupling.

### 2. Modified GameService

**Before**:
```java
@Service
public class GameService {
    private final SchedulerService schedulerService; // Circular dependency!
    
    public GameService(PlayerService playerService, SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
    
    private void someMethod() {
        schedulerService.queueGameUpdate(gameId); // Direct call
    }
}
```

**After**:
```java
@Service
public class GameService {
    private final ApplicationEventPublisher eventPublisher; // No dependency on SchedulerService!
    
    public GameService(PlayerService playerService, ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    private void publishGameUpdate(String gameId) {
        Game game = games.get(gameId);
        if (game != null) {
            eventPublisher.publishEvent(new GameUpdateEvent(this, game));
        }
    }
}
```

**Benefits**:
- GameService no longer depends on SchedulerService
- Uses Spring's built-in `ApplicationEventPublisher`
- Publishes events instead of making direct service calls

### 3. Modified SchedulerService

**Before**:
```java
@Service
public class SchedulerService {
    private final GameService gameService; // Circular dependency!
    
    public SchedulerService(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
    }
    
    private void broadcastPendingUpdates() {
        for (String gameId: pendingGameUpdates) {
            Optional<Game> gameOpt = gameService.getGameById(gameId); // Querying GameService
            // ...
        }
    }
}
```

**After**:
```java
@Service
public class SchedulerService {
    // No dependency on GameService!
    
    public SchedulerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @EventListener
    public void handleGameUpdateEvent(GameUpdateEvent event) {
        queueGameUpdate(event.getGame()); // Receives game from event
    }
    
    private void broadcastPendingUpdates() {
        for (Game game : pendingGameUpdates.values()) {
            messagingTemplate.convertAndSend("/topic/game/" + game.getId(), game);
        }
    }
}
```

**Benefits**:
- SchedulerService no longer depends on GameService
- Uses `@EventListener` to react to game updates
- Receives complete game data through events (no need to query)

## Dependency Graph

### Before (Circular):
```
GameService ←→ SchedulerService
     ↓              ↓
PlayerService  SimpMessagingTemplate
```

### After (Acyclic):
```
GameService → ApplicationEventPublisher
     ↓                     ↓
PlayerService    (publishes GameUpdateEvent)
                           ↓
                    SchedulerService (listens)
                           ↓
                  SimpMessagingTemplate
```

## Alternative Solutions Considered

### 1. Lazy Injection (`@Lazy`)
```java
public GameService(PlayerService playerService, @Lazy SchedulerService schedulerService) {
    // ...
}
```
**Why Not Used**: 
- Only hides the problem, doesn't solve it
- Still maintains tight coupling
- Makes the code less predictable
- Not a proper architectural solution

### 2. Setter Injection
```java
@Autowired
public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
}
```
**Why Not Used**:
- Breaks immutability (fields can't be final)
- Still maintains circular dependency
- Makes the dependency optional, which is misleading

### 3. Mediator Pattern
Create a separate mediator service to coordinate between GameService and SchedulerService.

**Why Not Used**:
- More complex than needed for this use case
- Event-driven architecture is simpler and more idiomatic in Spring
- Would introduce an additional service

## Best Practices for Avoiding Circular Dependencies

1. **Follow Single Responsibility Principle**: Each service should have one clear purpose
2. **Use Events for Cross-Cutting Concerns**: Broadcasting, logging, auditing are good candidates
3. **Consider the Dependency Direction**: Dependencies should flow in one direction (usually toward infrastructure)
4. **Design with Interfaces**: Use interfaces to define contracts and reduce coupling
5. **Apply the Dependency Inversion Principle**: Depend on abstractions, not concretions
6. **Review Dependency Graphs**: Regularly check your dependency structure
7. **Refactor Early**: Don't wait for circular dependencies to become a problem

## Testing the Solution

A unit test (`CircularDependencyResolutionTest`) was created to verify:
1. GameService can be instantiated without SchedulerService
2. SchedulerService can be instantiated without GameService
3. Events are properly published and handled

## Conclusion

The event-driven approach is the **industry standard** for resolving circular dependencies in Spring applications. It provides:
- Complete decoupling of services
- Better testability
- Improved maintainability
- Alignment with SOLID principles
- Natural integration with Spring Framework

This solution is not just a workaround—it's a proper architectural improvement that makes the codebase more maintainable and extensible.
